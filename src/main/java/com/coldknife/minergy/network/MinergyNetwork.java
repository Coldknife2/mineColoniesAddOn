package com.coldknife.minergy.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MinergyNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    private MinergyNetwork()
    {
    }

    public static void register(final RegisterPayloadHandlersEvent event)
    {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(MinergySyncMessage.TYPE, MinergySyncMessage.STREAM_CODEC, MinergySyncMessage::handle);
        registrar.playToServer(BuyBoostMessage.TYPE, BuyBoostMessage.STREAM_CODEC, BuyBoostMessage::handle);
    }

    public static void sendToServer(final CustomPacketPayload message)
    {
        PacketDistributor.sendToServer(message);
    }

    public static void sendTo(final ServerPlayer player, final CustomPacketPayload message)
    {
        PacketDistributor.sendToPlayer(player, message);
    }
}
