package com.coldknife.minergy.core;

import com.coldknife.minergy.marks.MinergyMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Game bus events: mark bookkeeping, server tick.
 */
public final class MinergyEvents
{
    /**
     * A block placed by a player or an entity replaces any old mark.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaced(final BlockEvent.EntityPlaceEvent event)
    {
        if (event.getLevel() instanceof Level level)
        {
            MinergyMarks.unmark(level, event.getPos());
        }
    }

    /**
     * Drops are computed after this event: the mark is checked again at the end of the tick.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBroken(final BlockEvent.BreakEvent event)
    {
        if (event.getLevel() instanceof Level level)
        {
            MinergyMarks.scheduleCheck(level, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onExplosion(final ExplosionEvent.Detonate event)
    {
        for (final BlockPos pos : event.getAffectedBlocks())
        {
            MinergyMarks.scheduleCheck(event.getLevel(), pos);
        }
    }

    /**
     * The mark follows a block pushed or pulled by a piston. Destroyed blocks keep their mark until their drops are computed.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPistonMove(final PistonEvent.Pre event)
    {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide())
        {
            return;
        }
        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.RETRACT && !level.getBlockState(event.getPos()).is(Blocks.STICKY_PISTON))
        {
            return;
        }

        final PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve())
        {
            return;
        }
        MinergyMarks.moveMarks(level, resolver.getToPush(), resolver.getPushDirection());
        for (final BlockPos pos : resolver.getToDestroy())
        {
            MinergyMarks.scheduleCheck(level, pos);
        }
    }

    @SubscribeEvent
    public void onServerTick(final ServerTickEvent.Post event)
    {
        MinergyMarks.processPendingChecks(event.getServer());
        MinergyManager.onServerTick();
    }
}
