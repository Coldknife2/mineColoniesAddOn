package com.coldknife.minergy.network;

import com.coldknife.minergy.client.ClientMinergyData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server to client: Minergy state of one colony.
 */
public record MinergySyncMessage(ResourceKey<Level> dimension, int colonyId, long amount, long capacity, double rate, boolean constructionEnabled,
                                 boolean minergyFirst, List<BoostInfo> boosts)
{
    /**
     * Active boost of a citizen: total bonus in percent and seconds left.
     */
    public record BoostInfo(int citizenId, int bonusPercent, int secondsLeft)
    {
    }

    public static void encode(final MinergySyncMessage message, final FriendlyByteBuf buf)
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

    public static MinergySyncMessage decode(final FriendlyByteBuf buf)
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

    public static void handle(final MinergySyncMessage message, final Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMinergyData.update(message)));
        context.get().setPacketHandled(true);
    }
}
