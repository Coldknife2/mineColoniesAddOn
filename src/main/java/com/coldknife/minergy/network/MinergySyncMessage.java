package com.coldknife.minergy.network;

import com.coldknife.minergy.client.ClientMinergyData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: Minergy state of one colony.
 */
public record MinergySyncMessage(ResourceKey<Level> dimension, int colonyId, long amount, long capacity, double rate, boolean constructionEnabled,
                                 boolean minergyFirst)
{
    public static void encode(final MinergySyncMessage message, final FriendlyByteBuf buf)
    {
        buf.writeResourceKey(message.dimension);
        buf.writeVarInt(message.colonyId);
        buf.writeVarLong(message.amount);
        buf.writeLong(message.capacity);
        buf.writeDouble(message.rate);
        buf.writeBoolean(message.constructionEnabled);
        buf.writeBoolean(message.minergyFirst);
    }

    public static MinergySyncMessage decode(final FriendlyByteBuf buf)
    {
        return new MinergySyncMessage(buf.readResourceKey(Registries.DIMENSION),
            buf.readVarInt(),
            buf.readVarLong(),
            buf.readLong(),
            buf.readDouble(),
            buf.readBoolean(),
            buf.readBoolean());
    }

    public static void handle(final MinergySyncMessage message, final Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMinergyData.update(message)));
        context.get().setPacketHandled(true);
    }
}
