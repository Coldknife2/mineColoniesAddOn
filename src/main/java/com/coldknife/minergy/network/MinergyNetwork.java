package com.coldknife.minergy.network;

import com.coldknife.minergy.Minergy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class MinergyNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Minergy.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals);

    private MinergyNetwork()
    {
    }

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++,
            MinergySyncMessage.class,
            MinergySyncMessage::encode,
            MinergySyncMessage::decode,
            MinergySyncMessage::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendTo(final ServerPlayer player, final Object message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
