package com.coldknife.minergy.client;

import com.coldknife.minergy.network.MinergySyncMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Last Minergy state received from the server, per colony.
 */
public final class ClientMinergyData
{
    private static final Map<String, MinergySyncMessage> DATA = new HashMap<>();

    private ClientMinergyData()
    {
    }

    private static String key(final ResourceKey<Level> dimension, final int colonyId)
    {
        return dimension.location() + "|" + colonyId;
    }

    public static void update(final MinergySyncMessage message)
    {
        DATA.put(key(message.dimension(), message.colonyId()), message);
    }

    @Nullable
    public static MinergySyncMessage get(final ResourceKey<Level> dimension, final int colonyId)
    {
        return DATA.get(key(dimension, colonyId));
    }

    public static void clear()
    {
        DATA.clear();
    }
}
