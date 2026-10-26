package com.coldknife.minergy.marks;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Positions of a chunk where a block was placed with Minergy, with the block that was placed.
 * Stored as a chunk capability so it is saved and loaded with the chunk itself.
 */
public class ChunkMinergyMarks
{
    private final Long2ObjectOpenHashMap<Block> marks = new Long2ObjectOpenHashMap<>();

    public void put(final long pos, final Block block)
    {
        marks.put(pos, block);
    }

    @Nullable
    public Block get(final long pos)
    {
        return marks.get(pos);
    }

    public boolean remove(final long pos)
    {
        return marks.remove(pos) != null;
    }

    public boolean isEmpty()
    {
        return marks.isEmpty();
    }

    /**
     * Serialized as {blockId: [packed positions]} to stay compact.
     */
    public CompoundTag serialize()
    {
        final Map<Block, LongArrayList> byBlock = new HashMap<>();
        for (final Long2ObjectOpenHashMap.Entry<Block> entry : marks.long2ObjectEntrySet())
        {
            byBlock.computeIfAbsent(entry.getValue(), b -> new LongArrayList()).add(entry.getLongKey());
        }

        final CompoundTag tag = new CompoundTag();
        for (final Map.Entry<Block, LongArrayList> entry : byBlock.entrySet())
        {
            tag.put(BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString(), new LongArrayTag(entry.getValue().toLongArray()));
        }
        return tag;
    }

    public void deserialize(final CompoundTag tag)
    {
        marks.clear();
        for (final String key : tag.getAllKeys())
        {
            final ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null)
            {
                continue;
            }
            final Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == Blocks.AIR)
            {
                continue;
            }
            for (final long pos : tag.getLongArray(key))
            {
                marks.put(pos, block);
            }
        }
    }
}
