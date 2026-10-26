package com.coldknife.minergy.ai;

import com.coldknife.minergy.colony.JobMysticalMage;
import com.coldknife.minergy.core.MinergyConfig;
import com.coldknife.minergy.core.MinergyManager;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildings.BuildingMysticalSite;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

/**
 * The Mystical Mage meditates at the Mystical Site and turns its concentration into Minergy, every second of work.
 */
public class EntityAIWorkMysticalMage extends AbstractEntityAIInteract<JobMysticalMage, BuildingMysticalSite>
{
    /**
     * Skill level giving the full skill bonus.
     */
    private static final double MAX_SKILL = 99.0D;

    /**
     * Seconds spent on a meditation spot before moving to another one.
     */
    private static final int SECONDS_PER_SPOT = 30;

    /**
     * Max seconds spent trying to reach a meditation spot.
     */
    private static final int MAX_WALK_SECONDS = 10;

    /**
     * Experience gained per second of meditation.
     */
    private static final double XP_PER_SECOND = 0.05D;

    /**
     * Seconds between two saturation decreases.
     */
    private static final int SECONDS_PER_SATURATION = 60;

    /**
     * One chance out of this, every second, to say something about the Minergy.
     */
    private static final int DIALOGUE_CHANCE = 900;

    private static final String[] DIALOGUES = {
        "com.minergy.mage.dialogue.flow",
        "com.minergy.mage.dialogue.stone",
        "com.minergy.mage.dialogue.builders",
        "com.minergy.mage.dialogue.ephemeral",
        "com.minergy.mage.dialogue.colony",
        "com.minergy.mage.dialogue.focus"
    };

    private static final String DIALOGUE_FULL = "com.minergy.mage.dialogue.full";

    private BlockPos meditationSpot  = null;
    private int      walkSeconds     = 0;
    private int      spotSeconds     = 0;
    private int      secondsWorked   = 0;

    public EntityAIWorkMysticalMage(@NotNull final JobMysticalMage job)
    {
        super(job);
        super.registerTargets(
            new AITarget(IDLE, START_WORKING, 1),
            new AITarget(START_WORKING, this::startWorkingAtOwnBuilding, TICKS_SECOND),
            new AITarget(MageState.MEDITATE, this::meditate, TICKS_SECOND)
        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    public Class<BuildingMysticalSite> getExpectedBuildingClass()
    {
        return BuildingMysticalSite.class;
    }

    /**
     * Minergy per second, from the site level and the mage's Mana.
     */
    public static double computeRate(final int buildingLevel, final int manaLevel)
    {
        final double base = MinergyConfig.BASE_RATE.get() + MinergyConfig.RATE_PER_LEVEL.get() * Math.max(0, buildingLevel - 1);
        final double skillBonus = 1.0D + MinergyConfig.MAX_SKILL_BONUS.get() * Math.min(MAX_SKILL, Math.max(0, manaLevel)) / MAX_SKILL;
        return base * skillBonus * MinergyConfig.RATE_MULTIPLIER.get();
    }

    private IAIState startWorkingAtOwnBuilding()
    {
        if (!walkToBuilding())
        {
            return getState();
        }
        pickMeditationSpot();
        return MageState.MEDITATE;
    }

    private void pickMeditationSpot()
    {
        final Tuple<BlockPos, BlockPos> corners = building.getCorners();
        final BlockPos hut = building.getPosition();
        if (corners == null)
        {
            meditationSpot = hut;
        }
        else
        {
            final BlockPos a = corners.getA();
            final BlockPos b = corners.getB();
            final int minX = Math.min(a.getX(), b.getX()) + 1;
            final int maxX = Math.max(a.getX(), b.getX()) - 1;
            final int minZ = Math.min(a.getZ(), b.getZ()) + 1;
            final int maxZ = Math.max(a.getZ(), b.getZ()) - 1;
            meditationSpot = maxX <= minX || maxZ <= minZ
                               ? hut
                               : new BlockPos(minX + worker.getRandom().nextInt(maxX - minX + 1), hut.getY(), minZ + worker.getRandom().nextInt(maxZ - minZ + 1));
        }
        walkSeconds = 0;
        spotSeconds = 0;
    }

    private IAIState meditate()
    {
        if (meditationSpot == null)
        {
            pickMeditationSpot();
        }

        if (!walkToWorkPos(meditationSpot) && walkSeconds < MAX_WALK_SECONDS)
        {
            walkSeconds++;
            return getState();
        }

        final IColony colony = building.getColony();
        final double rate = computeRate(building.getBuildingLevel(), getPrimarySkillLevel());
        MinergyManager.generate(colony, rate);
        MinergyManager.reportRate(colony, worker.getCitizenData().getId(), rate);

        worker.getCitizenExperienceHandler().addExperience(XP_PER_SECOND);
        worker.getNavigation().stop();
        spawnParticles();

        secondsWorked++;
        if (secondsWorked % SECONDS_PER_SATURATION == 0)
        {
            worker.decreaseSaturationForContinuousAction();
            worker.queueSound(SoundEvents.ENCHANTMENT_TABLE_USE, worker.blockPosition(), 40, 0, 0.5f, 1.2f);
        }

        if (worker.getRandom().nextInt(DIALOGUE_CHANCE) == 0)
        {
            final boolean full = MinergyManager.getAmount(colony) >= MinergyManager.getCapacity(colony);
            final String line = full ? DIALOGUE_FULL : DIALOGUES[worker.getRandom().nextInt(DIALOGUES.length)];
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(line), ChatPriority.CHITCHAT));
        }

        if (++spotSeconds >= SECONDS_PER_SPOT)
        {
            meditationSpot = null;
            return START_WORKING;
        }
        return getState();
    }

    private void spawnParticles()
    {
        if (world instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, worker.getX(), worker.getY() + 1.8D, worker.getZ(), 6, 0.6D, 0.4D, 0.6D, 0.4D);
            serverLevel.sendParticles(ParticleTypes.WITCH, worker.getX(), worker.getY() + 1.0D, worker.getZ(), 1, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }
}
