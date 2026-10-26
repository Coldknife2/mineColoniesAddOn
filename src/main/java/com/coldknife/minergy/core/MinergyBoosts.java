package com.coldknife.minergy.core;

import com.coldknife.minergy.Minergy;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary speed boosts bought with Minergy for one citizen: faster work (shorter AI delays) and faster walking.
 * <p>
 * Each purchase is independent and lasts the configured duration; the bonuses of the active purchases add up.
 */
public final class MinergyBoosts
{
    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("3f6c1d5e-8a2b-4c7d-9e10-6b5a4c3d2e1f");
    private static final String SPEED_MODIFIER_NAME = Minergy.MOD_ID + ":boost";

    /**
     * Active purchases per citizen, keyed by "dimension|colonyId|citizenId".
     */
    private static final Map<String, List<Boost>> BOOSTS = new HashMap<>();

    private MinergyBoosts()
    {
    }

    public enum Result
    {
        SUCCESS,
        NOT_ENOUGH_MINERGY,
        UNKNOWN_CITIZEN
    }

    private static String key(final IColony colony, final int citizenId)
    {
        return MinergyManager.key(colony) + "|" + citizenId;
    }

    public static long cost(final int steps)
    {
        return (long) steps * MinergyConfig.BOOST_COST_STEP.get();
    }

    public static double bonus(final int steps)
    {
        return steps * MinergyConfig.BOOST_BONUS_PER_STEP.get();
    }

    /**
     * Buys a boost of the given number of steps for a citizen of the colony.
     */
    public static Result buy(final IColony colony, final int citizenId, final int steps)
    {
        final ICitizenData citizen = colony.getCitizenManager().getCivilian(citizenId);
        if (citizen == null)
        {
            return Result.UNKNOWN_CITIZEN;
        }
        if (!MinergyManager.tryConsume(colony, cost(steps)))
        {
            return Result.NOT_ENOUGH_MINERGY;
        }
        final long end = colony.getWorld().getGameTime() + MinergyConfig.BOOST_DURATION_SECONDS.get() * 20L;
        BOOSTS.computeIfAbsent(key(colony, citizenId), k -> new ArrayList<>()).add(new Boost(bonus(steps), end));
        citizen.getEntity().ifPresent(entity -> applySpeed(entity, getBonus(colony, citizenId)));
        return Result.SUCCESS;
    }

    /**
     * @return the total bonus of the active boosts of a citizen (0.5 = +50%).
     */
    public static double getBonus(final IColony colony, final int citizenId)
    {
        if (BOOSTS.isEmpty())
        {
            return 0.0D;
        }
        final List<Boost> boosts = BOOSTS.get(key(colony, citizenId));
        if (boosts == null)
        {
            return 0.0D;
        }
        final long now = colony.getWorld().getGameTime();
        double total = 0.0D;
        for (final Boost boost : boosts)
        {
            if (boost.end > now)
            {
                total += boost.bonus;
            }
        }
        return total;
    }

    /**
     * @return the bonus of the worker of an AI, 0 if not boosted.
     */
    public static double getBonus(@Nullable final AbstractEntityCitizen worker)
    {
        if (BOOSTS.isEmpty() || worker == null || worker.getCitizenData() == null)
        {
            return 0.0D;
        }
        final IColony colony = worker.getCitizenColonyHandler().getColony();
        return colony == null ? 0.0D : getBonus(colony, worker.getCitizenData().getId());
    }

    /**
     * @return the seconds left before the last active boost of a citizen ends, 0 if none.
     */
    public static int getSecondsLeft(final IColony colony, final int citizenId)
    {
        final List<Boost> boosts = BOOSTS.get(key(colony, citizenId));
        if (boosts == null)
        {
            return 0;
        }
        final long now = colony.getWorld().getGameTime();
        long last = now;
        for (final Boost boost : boosts)
        {
            last = Math.max(last, boost.end);
        }
        return (int) ((last - now + 19) / 20);
    }

    /**
     * Citizen ids of the colony with an active boost.
     */
    public static List<Integer> getBoostedCitizens(final IColony colony)
    {
        final String prefix = MinergyManager.key(colony) + "|";
        final List<Integer> ids = new ArrayList<>();
        for (final String key : BOOSTS.keySet())
        {
            if (key.startsWith(prefix))
            {
                ids.add(Integer.parseInt(key.substring(prefix.length())));
            }
        }
        return ids;
    }

    /**
     * Every second: drops the ended boosts, keeps the walking speed modifier up to date and shows the boost.
     */
    public static void tick(final IColony colony)
    {
        final List<Integer> boosted = getBoostedCitizens(colony);
        if (!boosted.isEmpty())
        {
            // The countdown is shown in the Boost tab.
            MinergyManager.markDirty(colony);
        }
        for (final int citizenId : boosted)
        {
            final String key = key(colony, citizenId);
            final List<Boost> boosts = BOOSTS.get(key);
            final long now = colony.getWorld().getGameTime();
            for (final Iterator<Boost> it = boosts.iterator(); it.hasNext(); )
            {
                if (it.next().end <= now)
                {
                    it.remove();
                }
            }
            final double bonus = getBonus(colony, citizenId);
            if (boosts.isEmpty())
            {
                BOOSTS.remove(key);
            }

            final ICitizenData citizen = colony.getCitizenManager().getCivilian(citizenId);
            if (citizen == null)
            {
                BOOSTS.remove(key);
                continue;
            }
            citizen.getEntity().ifPresent(entity -> {
                applySpeed(entity, bonus);
                if (bonus > 0 && entity.level() instanceof ServerLevel level)
                {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 4, 0.3D, 0.5D, 0.3D, 0.05D);
                }
            });
        }
    }

    private static void applySpeed(final AbstractEntityCitizen entity, final double bonus)
    {
        final AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
        {
            return;
        }
        final AttributeModifier current = speed.getModifier(SPEED_MODIFIER_ID);
        if (current != null && current.getAmount() == bonus)
        {
            return;
        }
        speed.removeModifier(SPEED_MODIFIER_ID);
        if (bonus > 0)
        {
            speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, SPEED_MODIFIER_NAME, bonus, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    public static void onColonyDeleted(final IColony colony)
    {
        final String prefix = MinergyManager.key(colony) + "|";
        BOOSTS.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private record Boost(double bonus, long end)
    {
    }
}
