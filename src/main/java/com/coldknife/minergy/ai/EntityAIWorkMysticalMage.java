package com.coldknife.minergy.ai;

import com.coldknife.minergy.colony.JobMysticalMage;
import com.coldknife.minergy.core.MinergyBoosts;
import com.coldknife.minergy.core.MinergyConfig;
import com.coldknife.minergy.core.MinergyManager;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLibrary;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.entity.other.SittingEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

/**
 * The Mystical Mage sits down near a bookshelf of the Library and turns its concentration into Minergy, every second of work.
 * When the colony's Minergy goes above the Library's auto boost threshold, it stands up and walks around the colony to boost
 * the workers (builders first), until the stock falls back below the threshold.
 */
public class EntityAIWorkMysticalMage extends AbstractEntityAIInteract<JobMysticalMage, BuildingLibrary>
{
    /**
     * Skill level giving the full skill bonus.
     */
    private static final double MAX_SKILL = 99.0D;

    /**
     * How long one sitting lasts; the mage sits down again on the same spot right after, without walking around.
     * Kept short so that a mage called away (food, sleep, danger) does not stay seated for long.
     */
    private static final int SITTING_SECONDS = 60;

    /**
     * Max seconds spent trying to reach the meditation spot.
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

    private static final String DIALOGUE_BOOST = "com.minergy.mage.dialogue.boost";

    /**
     * Seconds between two checks of the auto boost threshold while meditating.
     */
    private static final int AUTO_BOOST_CHECK_SECONDS = 10;

    /**
     * Max seconds spent walking to one worker before choosing another one.
     */
    private static final int MAX_BOOST_WALK_SECONDS = 60;

    /**
     * Distance (blocks) to the worker to give it the boost.
     */
    private static final int BOOST_REACH = 3;

    /**
     * Seconds of meditation when no worker can be boosted, before looking again.
     */
    private static final int NO_TARGET_COOLDOWN_SECONDS = 60;

    /**
     * Where the mage walks to before sitting down (a bookshelf of the Library).
     */
    private BlockPos walkTarget    = null;

    /**
     * Where the mage sits, kept for the whole work session so it does not wander.
     */
    private BlockPos seat          = null;

    private int      walkSeconds   = 0;
    private int      secondsWorked = 0;

    /**
     * Worker the mage walks to, to boost it.
     */
    @Nullable
    private ICitizenData boostTarget      = null;
    private int          boostWalkSeconds = 0;
    private int          boostCooldown    = 0;

    public EntityAIWorkMysticalMage(@NotNull final JobMysticalMage job)
    {
        super(job);
        super.registerTargets(
            new AITarget(IDLE, START_WORKING, 1),
            new AITarget(START_WORKING, this::startWorkingAtOwnBuilding, TICKS_SECOND),
            new AITarget(MageState.MEDITATE, this::meditate, TICKS_SECOND),
            new AITarget(MageState.BOOST_WORKERS, this::boostWorkers, TICKS_SECOND)
        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    public Class<BuildingLibrary> getExpectedBuildingClass()
    {
        return BuildingLibrary.class;
    }

    /**
     * Minergy per second, from the Library level and the mage's Mana.
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
        walkTarget = building.getRandomBookShelf();
        seat = null;
        walkSeconds = 0;
        return MageState.MEDITATE;
    }

    private IAIState meditate()
    {
        if (boostCooldown > 0)
        {
            boostCooldown--;
        }
        else if (secondsWorked % AUTO_BOOST_CHECK_SECONDS == 0 && shouldBoostWorkers())
        {
            standUp();
            boostTarget = null;
            return MageState.BOOST_WORKERS;
        }

        if (seat == null)
        {
            if (!walkToWorkPos(walkTarget) && walkSeconds < MAX_WALK_SECONDS)
            {
                walkSeconds++;
                return getState();
            }
            seat = worker.blockPosition();
        }

        if (worker.getVehicle() == null)
        {
            worker.getNavigation().stop();
            SittingEntity.sitDown(seat, worker, SITTING_SECONDS * TICKS_SECOND);
        }

        final IColony colony = building.getColony();
        final double rate = computeRate(building.getBuildingLevel(), getPrimarySkillLevel());
        MinergyManager.generate(colony, rate);
        MinergyManager.reportRate(colony, worker.getCitizenData().getId(), rate);

        worker.getCitizenExperienceHandler().addExperience(XP_PER_SECOND);
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

        return getState();
    }

    /**
     * @return true if the colony has more Minergy than the Library's auto boost threshold.
     */
    private boolean shouldBoostWorkers()
    {
        final IColony colony = building.getColony();
        final int threshold = MinergyManager.getAutoBoostThreshold(colony);
        return threshold > 0 && MinergyManager.getAmount(colony) >= threshold;
    }

    private void standUp()
    {
        if (worker.getVehicle() != null)
        {
            worker.stopRiding();
        }
        seat = null;
    }

    /**
     * Walks to a worker, boosts it, then picks the next one while the Minergy stays above the threshold.
     */
    private IAIState boostWorkers()
    {
        if (!shouldBoostWorkers())
        {
            boostTarget = null;
            return START_WORKING;
        }

        if (boostTarget == null || boostTarget.getEntity().isEmpty())
        {
            boostTarget = pickBoostTarget();
            boostWalkSeconds = 0;
            if (boostTarget == null)
            {
                // Everybody is boosted already: back to meditation for a while.
                boostCooldown = NO_TARGET_COOLDOWN_SECONDS;
                return START_WORKING;
            }
        }

        final AbstractEntityCitizen target = boostTarget.getEntity().get();
        if (!EntityNavigationUtils.walkToPos(worker, target.blockPosition(), BOOST_REACH, false)
              && worker.distanceToSqr(target) > BOOST_REACH * BOOST_REACH)
        {
            if (++boostWalkSeconds > MAX_BOOST_WALK_SECONDS)
            {
                boostTarget = null;
            }
            return getState();
        }

        final IColony colony = building.getColony();
        if (MinergyBoosts.buy(colony, boostTarget.getId(), MinergyManager.getAutoBoostSteps(colony)) == MinergyBoosts.Result.SUCCESS)
        {
            worker.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            worker.getCitizenExperienceHandler().addExperience(XP_PER_SECOND * 20);
            worker.queueSound(SoundEvents.ENCHANTMENT_TABLE_USE, target.blockPosition(), 20, 0, 0.8f, 1.5f);
            if (world instanceof ServerLevel serverLevel)
            {
                serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.2D, target.getZ(), 30, 0.5D, 0.6D, 0.5D, 0.8D);
            }
            if (worker.getRandom().nextInt(4) == 0)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(DIALOGUE_BOOST), ChatPriority.CHITCHAT));
            }
        }
        boostTarget = null;
        return getState();
    }

    /**
     * @return a worker of the colony without an active boost, builders first, or null if none.
     */
    @Nullable
    private ICitizenData pickBoostTarget()
    {
        final IColony colony = building.getColony();
        final List<ICitizenData> builders = new ArrayList<>();
        final List<ICitizenData> others = new ArrayList<>();
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (citizen.getJob() == null
                  || citizen.getJob() instanceof JobMysticalMage
                  || citizen.getEntity().isEmpty()
                  || MinergyBoosts.getBonus(colony, citizen.getId()) > 0)
            {
                continue;
            }
            if (citizen.getJob().getJobRegistryEntry().getKey().getPath().contains("builder"))
            {
                builders.add(citizen);
            }
            else
            {
                others.add(citizen);
            }
        }
        final List<ICitizenData> pool = builders.isEmpty() ? others : builders;
        return pool.isEmpty() ? null : pool.get(worker.getRandom().nextInt(pool.size()));
    }

    private void spawnParticles()
    {
        if (world instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, worker.getX(), worker.getY() + 1.4D, worker.getZ(), 6, 0.6D, 0.4D, 0.6D, 0.4D);
            serverLevel.sendParticles(ParticleTypes.WITCH, worker.getX(), worker.getY() + 0.8D, worker.getZ(), 1, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }
}
