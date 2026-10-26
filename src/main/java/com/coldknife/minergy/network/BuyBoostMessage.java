package com.coldknife.minergy.network;

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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server: buy a speed boost for a citizen with the colony's Minergy.
 */
public record BuyBoostMessage(ResourceKey<Level> dimension, int colonyId, int citizenId, int steps)
{
    public static void encode(final BuyBoostMessage message, final FriendlyByteBuf buf)
    {
        buf.writeResourceKey(message.dimension);
        buf.writeVarInt(message.colonyId);
        buf.writeVarInt(message.citizenId);
        buf.writeVarInt(message.steps);
    }

    public static BuyBoostMessage decode(final FriendlyByteBuf buf)
    {
        return new BuyBoostMessage(buf.readResourceKey(Registries.DIMENSION), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(final BuyBoostMessage message, final Supplier<NetworkEvent.Context> context)
    {
        final ServerPlayer player = context.get().getSender();
        context.get().enqueueWork(() -> handleOnServer(message, player));
        context.get().setPacketHandled(true);
    }

    private static void handleOnServer(final BuyBoostMessage message, final ServerPlayer player)
    {
        if (player == null)
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
}
