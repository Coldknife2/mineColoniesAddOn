package com.coldknife.minergy.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * World-wide storage of the Minergy of every colony, kept in the overworld data folder.
 * Keys are "dimension|colonyId" so a colony's Minergy can never be used by another colony.
 */
public class MinergySavedData extends SavedData
{
    private static final String NAME       = "minergy_colonies";
    private static final String TAG_LIST   = "colonies";
    private static final String TAG_KEY    = "key";
    private static final String TAG_AMOUNT = "amount";

    private final Map<String, Double> amounts = new HashMap<>();

    public static MinergySavedData get(final MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MinergySavedData::new, MinergySavedData::load), NAME);
    }

    private static MinergySavedData load(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        final MinergySavedData data = new MinergySavedData();
        final ListTag list = tag.getList(TAG_LIST, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            final CompoundTag entry = list.getCompound(i);
            data.amounts.put(entry.getString(TAG_KEY), entry.getDouble(TAG_AMOUNT));
        }
        return data;
    }

    @NotNull
    @Override
    public CompoundTag save(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        final ListTag list = new ListTag();
        for (final Map.Entry<String, Double> entry : amounts.entrySet())
        {
            final CompoundTag compound = new CompoundTag();
            compound.putString(TAG_KEY, entry.getKey());
            compound.putDouble(TAG_AMOUNT, entry.getValue());
            list.add(compound);
        }
        tag.put(TAG_LIST, list);
        return tag;
    }

    public double getAmount(final String key)
    {
        return amounts.getOrDefault(key, 0.0D);
    }

    public void setAmount(final String key, final double amount)
    {
        amounts.put(key, Math.max(0.0D, amount));
        setDirty();
    }

    public void remove(final String key)
    {
        if (amounts.remove(key) != null)
        {
            setDirty();
        }
    }
}
