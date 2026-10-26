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

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: Minergy state of one colony.
 */
public record MinergySyncMessage(ResourceKey<Level> dimension, int colonyId, long amount, long capacity, double rate, boolean constructionEnabled,
                                 boolean minergyFirst, List<BoostInfo> boosts) implements CustomPacketPayload
{
    /**
     * Active boost of a citizen: total bonus in percent and seconds left.
     */
    public record BoostInfo(int citizenId, int bonusPercent, int secondsLeft)
    {
    }

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
        buf.writeVarInt(message.boosts.size());
        for (final BoostInfo boost : message.boosts)
        {
            buf.writeVarInt(boost.citizenId);
            buf.writeVarInt(boost.bonusPercent);
            buf.writeVarInt(boost.secondsLeft);
        }
    }

    private static MinergySyncMessage decode(final FriendlyByteBuf buf)
    {
        final ResourceKey<Level> dimension = buf.readResourceKey(Registries.DIMENSION);
        final int colonyId = buf.readVarInt();
        final long amount = buf.readVarLong();
        final long capacity = buf.readLong();
        final double rate = buf.readDouble();
        final boolean constructionEnabled = buf.readBoolean();
        final boolean minergyFirst = buf.readBoolean();
        final int count = buf.readVarInt();
        final List<BoostInfo> boosts = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            boosts.add(new BoostInfo(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new MinergySyncMessage(dimension, colonyId, amount, capacity, rate, constructionEnabled, minergyFirst, boosts);
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
