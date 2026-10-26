package com.coldknife.minergy.marks;

import com.coldknife.minergy.Minergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Access to the "placed with Minergy" marks. A marked block drops nothing when broken.
 */
public final class MinergyMarks
{
    public static final Capability<ChunkMinergyMarks> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Minergy.MOD_ID, "marks");

    /**
     * Positions to re-check at the end of the tick: the mark is removed if the marked block is gone.
     */
    private static final List<PendingCheck> PENDING = new ArrayList<>();

    private MinergyMarks()
    {
    }

    @Nullable
    private static ChunkMinergyMarks marks(final Level level, final BlockPos pos)
    {
        if (level.isClientSide() || !level.hasChunkAt(pos))
        {
            return null;
        }
        return level.getChunkAt(pos).getCapability(CAPABILITY).resolve().orElse(null);
    }

    /**
     * Marks the block at the position, and the other half of beds and doors/tall blocks.
     */
    public static void markPlaced(final Level level, final BlockPos pos)
    {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir())
        {
            return;
        }
        for (final BlockPos part : getParts(level, pos, state))
        {
            mark(level, part, state.getBlock());
        }
    }

    /**
     * Removes the marks of the block at the position and of its other half, if any.
     */
    public static void unmarkPlaced(final Level level, final BlockPos pos)
    {
        for (final BlockPos part : getParts(level, pos, level.getBlockState(pos)))
        {
            unmark(level, part);
        }
    }

    /**
     * @return the position and the position of the other half of a door, tall plant or bed.
     */
    private static List<BlockPos> getParts(final Level level, final BlockPos pos, final BlockState state)
    {
        final List<BlockPos> parts = new ArrayList<>();
        parts.add(pos);

        BlockPos other = null;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
        {
            other = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        }
        else if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
        {
            final Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            other = state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
        }

        if (other != null && level.getBlockState(other).is(state.getBlock()))
        {
            parts.add(other);
        }
        return parts;
    }

    public static void mark(final Level level, final BlockPos pos, final Block block)
    {
        final ChunkMinergyMarks marks = marks(level, pos);
        if (marks != null)
        {
            marks.put(pos.asLong(), block);
            level.getChunkAt(pos).setUnsaved(true);
        }
    }

    public static void unmark(final Level level, final BlockPos pos)
    {
        final ChunkMinergyMarks marks = marks(level, pos);
        if (marks != null && marks.remove(pos.asLong()))
        {
            level.getChunkAt(pos).setUnsaved(true);
        }
    }

    @Nullable
    public static Block getMark(final Level level, final BlockPos pos)
    {
        final ChunkMinergyMarks marks = marks(level, pos);
        return marks == null ? null : marks.get(pos.asLong());
    }

    /**
     * @return true if the given state at this position was placed with Minergy and must not drop anything.
     * A mark that does not match the block anymore is stale and gets removed.
     */
    public static boolean isNoDrop(final Level level, final BlockPos pos, final BlockState state)
    {
        final Block marked = getMark(level, pos);
        if (marked == null)
        {
            return false;
        }
        if (marked != state.getBlock())
        {
            unmark(level, pos);
            return false;
        }
        scheduleCheck(level, pos);
        return true;
    }

    /**
     * Re-checks the position at the end of the server tick, once the block was (maybe) removed.
     */
    public static void scheduleCheck(final Level level, final BlockPos pos)
    {
        if (!level.isClientSide())
        {
            PENDING.add(new PendingCheck(level.dimension(), pos.immutable()));
        }
    }

    public static void processPendingChecks(final MinecraftServer server)
    {
        if (PENDING.isEmpty())
        {
            return;
        }
        final List<PendingCheck> checks = new ArrayList<>(PENDING);
        PENDING.clear();
        for (final PendingCheck check : checks)
        {
            final ServerLevel level = server.getLevel(check.dimension);
            if (level == null)
            {
                continue;
            }
            final Block marked = getMark(level, check.pos);
            if (marked != null && !level.getBlockState(check.pos).is(marked))
            {
                unmark(level, check.pos);
            }
        }
    }

    /**
     * Moves the marks of blocks pushed or pulled by a piston.
     */
    public static void moveMarks(final Level level, final List<BlockPos> moved, final Direction direction)
    {
        final List<BlockPos> sources = new ArrayList<>();
        final List<Block> blocks = new ArrayList<>();
        for (final BlockPos pos : moved)
        {
            final Block marked = getMark(level, pos);
            if (marked != null)
            {
                sources.add(pos);
                blocks.add(marked);
            }
        }
        for (final BlockPos pos : sources)
        {
            unmark(level, pos);
        }
        for (int i = 0; i < sources.size(); i++)
        {
            mark(level, sources.get(i).relative(direction), blocks.get(i));
        }
    }

    private record PendingCheck(ResourceKey<Level> dimension, BlockPos pos)
    {
    }

    /**
     * Capability provider attached to every server chunk.
     */
    public static class Provider implements ICapabilitySerializable<CompoundTag>
    {
        private final ChunkMinergyMarks                 marks    = new ChunkMinergyMarks();
        private final LazyOptional<ChunkMinergyMarks>   optional = LazyOptional.of(() -> marks);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull final Capability<T> cap, @Nullable final Direction side)
        {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return marks.serialize();
        }

        @Override
        public void deserializeNBT(final CompoundTag tag)
        {
            marks.deserialize(tag);
        }
    }
}
