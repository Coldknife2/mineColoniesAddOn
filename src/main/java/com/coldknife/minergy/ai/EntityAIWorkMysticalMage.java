package com.coldknife.minergy.ai;

import com.coldknife.minergy.colony.JobMysticalMage;
import com.coldknife.minergy.core.MinergyConfig;
import com.coldknife.minergy.core.MinergyManager;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLibrary;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.other.SittingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

/**
 * The Mystical Mage sits down near a bookshelf of the Library and turns its concentration into Minergy, every second of work.
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

    private void spawnParticles()
    {
        if (world instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, worker.getX(), worker.getY() + 1.4D, worker.getZ(), 6, 0.6D, 0.4D, 0.6D, 0.4D);
            serverLevel.sendParticles(ParticleTypes.WITCH, worker.getX(), worker.getY() + 0.8D, worker.getZ(), 1, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }
}
