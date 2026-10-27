package com.coldknife.minergy.core;

import com.coldknife.minergy.colony.MinergyBuildingModules;
import com.coldknife.minergy.network.MinergyNetwork;
import com.coldknife.minergy.network.MinergySyncMessage;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLibrary;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server-side entry point to read, generate and consume the Minergy of a colony.
 */
public final class MinergyManager
{
    /**
     * A mage that did not report its rate for this long is considered as not working anymore.
     */
    private static final long RATE_TIMEOUT_TICKS = 100;

    /**
     * Colonies are synced every second when changed, and every few seconds anyway (new subscribers).
     */
    private static final int SYNC_INTERVAL       = 20;
    private static final int FULL_SYNC_INTERVAL  = 100;

    /**
     * Current generation rate per colony key and citizen id, with the tick it was reported at.
     */
    private static final Map<String, Map<Integer, RateReport>> RATES = new HashMap<>();

    private static final Set<String> DIRTY = new HashSet<>();

    private static int tickCounter = 0;

    private MinergyManager()
    {
    }

    public static String key(final IColony colony)
    {
        return colony.getDimension().location() + "|" + colony.getID();
    }

    @Nullable
    private static MinergySavedData data()
    {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : MinergySavedData.get(server);
    }

    /**
     * @return the Minergy currently stored by the colony.
     */
    public static double getAmount(final IColony colony)
    {
        final MinergySavedData data = data();
        return data == null ? 0.0D : data.getAmount(key(colony));
    }

    /**
     * @return the best (highest level) Library of the colony, which holds the Minergy settings, or null if none.
     */
    @Nullable
    public static IBuilding getMainLibrary(final IColony colony)
    {
        IBuilding best = null;
        for (final IBuilding building : colony.getServerBuildingManager().getBuildings().values())
        {
            if (building instanceof BuildingLibrary && (best == null || building.getBuildingLevel() > best.getBuildingLevel()))
            {
                best = building;
            }
        }
        return best;
    }

    /**
     * @return the max Minergy the colony can hold, or {@link Long#MAX_VALUE} if unlimited.
     */
    public static long getCapacity(final IColony colony)
    {
        final long perLevel = MinergyConfig.STORAGE_PER_LEVEL.get();
        if (perLevel <= 0)
        {
            return Long.MAX_VALUE;
        }
        final IBuilding library = getMainLibrary(colony);
        return MinergyConfig.BASE_STORAGE.get() + (library == null ? 0 : perLevel * Math.max(1, library.getBuildingLevel()));
    }

    /**
     * @return the Minergy per second of the town hall, 0 if the colony has none.
     */
    public static double getTownHallRate(final IColony colony)
    {
        return colony.getServerBuildingManager().hasTownHall() ? MinergyConfig.TOWN_HALL_RATE.get() : 0.0D;
    }

    /**
     * Adds generated Minergy to the colony, capped by its storage.
     */
    public static void generate(final IColony colony, final double amount)
    {
        final MinergySavedData data = data();
        if (data == null || amount <= 0)
        {
            return;
        }
        final String key = key(colony);
        final double current = data.getAmount(key);
        final double capped = Math.min(current + amount, (double) getCapacity(colony));
        if (capped > current)
        {
            data.setAmount(key, capped);
            DIRTY.add(key);
        }
    }

    /**
     * Sends the colony's state to its close players at the next sync.
     */
    public static void markDirty(final IColony colony)
    {
        DIRTY.add(key(colony));
    }

    /**
     * Checks if the colony has at least the given amount of Minergy.
     */
    public static boolean has(final IColony colony, final long amount)
    {
        return getAmount(colony) >= amount;
    }

    /**
     * Consumes the given amount if available.
     *
     * @return true if consumed.
     */
    public static boolean tryConsume(final IColony colony, final long amount)
    {
        final MinergySavedData data = data();
        if (data == null)
        {
            return false;
        }
        final String key = key(colony);
        final double current = data.getAmount(key);
        if (current < amount)
        {
            return false;
        }
        data.setAmount(key, current - amount);
        DIRTY.add(key);
        return true;
    }

    /**
     * @return true if the colony's main Library allows Minergy for constructions (always true without a Library).
     */
    public static boolean isConstructionEnabled(final IColony colony)
    {
        // Without a Library, the town hall Minergy can already be used to build: the defaults apply.
        final IBuilding library = getMainLibrary(colony);
        return library == null || library.getSettingValueOrDefault(MinergyBuildingModules.CONSTRUCTION, true);
    }

    /**
     * @return true if the builders must use Minergy before their real blocks.
     */
    public static boolean isConstructionMinergyFirst(final IColony colony)
    {
        final IBuilding site = getMainLibrary(colony);
        return site != null
                 && MinergyBuildingModules.MODE_FIRST.equals(site.getSettingValueOrDefault(MinergyBuildingModules.CONSTRUCTION_MODE, MinergyBuildingModules.MODE_FALLBACK));
    }

    /**
     * @return the Minergy stock above which the mage boosts the workers, 0 if disabled.
     */
    public static int getAutoBoostThreshold(final IColony colony)
    {
        final IBuilding library = getMainLibrary(colony);
        return library == null ? 0 : Math.max(0, library.getSettingValueOrDefault(MinergyBuildingModules.AUTO_BOOST_THRESHOLD, 0));
    }

    /**
     * @return the number of boost steps the mage gives to each worker.
     */
    public static int getAutoBoostSteps(final IColony colony)
    {
        final IBuilding library = getMainLibrary(colony);
        final int steps = library == null ? 1 : library.getSettingValueOrDefault(MinergyBuildingModules.AUTO_BOOST_STEPS, 1);
        return Math.max(1, Math.min(MinergyConfig.BOOST_MAX_STEPS.get(), steps));
    }

    /**
     * Called by a working mage to publish its current generation rate.
     */
    public static void reportRate(final IColony colony, final int citizenId, final double ratePerSecond)
    {
        final long now = colony.getWorld().getGameTime();
        RATES.computeIfAbsent(key(colony), k -> new HashMap<>()).put(citizenId, new RateReport(ratePerSecond, now));
    }

    /**
     * @return the town hall rate plus the rates of the mages that worked recently.
     */
    public static double getRate(final IColony colony)
    {
        double total = getTownHallRate(colony);
        final Map<Integer, RateReport> reports = RATES.get(key(colony));
        if (reports == null)
        {
            return total;
        }
        final long now = colony.getWorld().getGameTime();
        reports.values().removeIf(report -> now - report.tick > RATE_TIMEOUT_TICKS);
        for (final RateReport report : reports.values())
        {
            total += report.rate;
        }
        return total;
    }

    /**
     * Forgets everything about a deleted colony.
     */
    public static void onColonyDeleted(final IColony colony)
    {
        final String key = key(colony);
        RATES.remove(key);
        DIRTY.remove(key);
        MinergyBoosts.onColonyDeleted(colony);
        final MinergySavedData data = data();
        if (data != null)
        {
            data.remove(key);
        }
    }

    /**
     * Server tick, every second: town hall generation, then the Minergy state sent to the players close to each colony.
     */
    public static void onServerTick()
    {
        tickCounter++;
        if (tickCounter % SYNC_INTERVAL != 0)
        {
            return;
        }
        final boolean full = tickCounter % FULL_SYNC_INTERVAL == 0;

        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            generate(colony, getTownHallRate(colony));
            MinergyBoosts.tick(colony);

            final String key = key(colony);
            if (!full && !DIRTY.contains(key))
            {
                continue;
            }
            DIRTY.remove(key);

            final Set<ServerPlayer> players = colony.getPackageManager().getCloseSubscribers();
            if (players.isEmpty())
            {
                continue;
            }
            final MinergySyncMessage message = createSyncMessage(colony);
            for (final ServerPlayer player : players)
            {
                MinergyNetwork.sendTo(player, message);
            }
        }
    }

    public static MinergySyncMessage createSyncMessage(final IColony colony)
    {
        return new MinergySyncMessage(colony.getDimension(),
            colony.getID(),
            (long) Math.floor(getAmount(colony)),
            getCapacity(colony),
            getRate(colony),
            isConstructionEnabled(colony),
            isConstructionMinergyFirst(colony),
            createBoostInfos(colony));
    }

    private static List<MinergySyncMessage.BoostInfo> createBoostInfos(final IColony colony)
    {
        final List<MinergySyncMessage.BoostInfo> infos = new ArrayList<>();
        for (final int citizenId : MinergyBoosts.getBoostedCitizens(colony))
        {
            final double bonus = MinergyBoosts.getBonus(colony, citizenId);
            if (bonus > 0)
            {
                infos.add(new MinergySyncMessage.BoostInfo(citizenId, (int) Math.round(bonus * 100), MinergyBoosts.getSecondsLeft(colony, citizenId)));
            }
        }
        return infos;
    }

    private record RateReport(double rate, long tick)
    {
    }
}
