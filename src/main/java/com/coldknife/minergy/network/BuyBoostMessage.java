package com.coldknife.minergy.network;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.core.MinergyBoosts;
import com.coldknife.minergy.core.MinergyConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Client to server: buy a speed boost for a citizen with the colony's Minergy.
 */
public record BuyBoostMessage(ResourceKey<Level> dimension, int colonyId, int citizenId, int steps) implements CustomPacketPayload
{
    public static final Type<BuyBoostMessage> TYPE = new Type<>(Minergy.id("buy_boost"));

    public static final StreamCodec<FriendlyByteBuf, BuyBoostMessage> STREAM_CODEC = StreamCodec.of(BuyBoostMessage::encode, BuyBoostMessage::decode);

    private static void encode(final FriendlyByteBuf buf, final BuyBoostMessage message)
    {
        buf.writeResourceKey(message.dimension);
        buf.writeVarInt(message.colonyId);
        buf.writeVarInt(message.citizenId);
        buf.writeVarInt(message.steps);
    }

    private static BuyBoostMessage decode(final FriendlyByteBuf buf)
    {
        return new BuyBoostMessage(buf.readResourceKey(Registries.DIMENSION), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    /**
     * Server side handling, on the main thread.
     */
    public static void handle(final BuyBoostMessage message, final IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player))
        {
            return;
        }
        final IColony colony = IColonyManager.getInstance().getColonyByDimension(message.colonyId, message.dimension);
        if (colony == null || !colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
        {
            return;
        }
        final int steps = Math.max(1, Math.min(message.steps, MinergyConfig.BOOST_MAX_STEPS.get()));
        final ICitizenData citizen = colony.getCitizenManager().getCivilian(message.citizenId);
        final MinergyBoosts.Result result = MinergyBoosts.buy(colony, message.citizenId, steps);
        switch (result)
        {
            case SUCCESS -> player.displayClientMessage(Component.translatable("com.minergy.boost.success",
                citizen.getName(),
                (int) Math.round(MinergyBoosts.bonus(steps) * 100),
                MinergyConfig.BOOST_DURATION_SECONDS.get(),
                MinergyBoosts.cost(steps)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            case NOT_ENOUGH_MINERGY -> player.displayClientMessage(Component.translatable("com.minergy.boost.notenough",
                MinergyBoosts.cost(steps)).withStyle(ChatFormatting.RED), false);
            case UNKNOWN_CITIZEN ->
            {
            }
        }
    }

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
