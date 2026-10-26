package com.coldknife.minergy.network;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.client.ClientMinergyData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Server to client: Minergy state of one colony.
 */
public record MinergySyncMessage(ResourceKey<Level> dimension, int colonyId, long amount, long capacity, double rate, boolean constructionEnabled,
                                 boolean minergyFirst) implements CustomPacketPayload
{
    public static final Type<MinergySyncMessage> TYPE = new Type<>(Minergy.id("sync"));

    public static final StreamCodec<FriendlyByteBuf, MinergySyncMessage> STREAM_CODEC = StreamCodec.of(MinergySyncMessage::encode, MinergySyncMessage::decode);

    private static void encode(final FriendlyByteBuf buf, final MinergySyncMessage message)
    {
        buf.writeResourceKey(message.dimension);
        buf.writeVarInt(message.colonyId);
        buf.writeVarLong(message.amount);
        buf.writeLong(message.capacity);
        buf.writeDouble(message.rate);
        buf.writeBoolean(message.constructionEnabled);
        buf.writeBoolean(message.minergyFirst);
    }

    private static MinergySyncMessage decode(final FriendlyByteBuf buf)
    {
        return new MinergySyncMessage(buf.readResourceKey(Registries.DIMENSION),
            buf.readVarInt(),
            buf.readVarLong(),
            buf.readLong(),
            buf.readDouble(),
            buf.readBoolean(),
            buf.readBoolean());
    }

    /**
     * Client side handling, on the main thread.
     */
    public static void handle(final MinergySyncMessage message, final IPayloadContext context)
    {
        ClientMinergyData.update(message);
    }

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
