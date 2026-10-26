package com.coldknife.minergy.client;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.network.MinergySyncMessage;
import com.ldtteam.blockui.BOScreen;
import com.ldtteam.structurize.client.gui.WindowExtendedBuildTool;
import com.ldtteam.structurize.items.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;

/**
 * Shows the colony's Minergy while using the Structurize build tool (build mode).
 */
public final class MinergyHud
{
    public static final ResourceLocation ICON = new ResourceLocation(Minergy.MOD_ID, "textures/gui/minergy.png");

    private static final int ICON_SIZE = 16;
    private static final int MARGIN    = 6;
    private static final int COLOR     = 0xFFC77DFF;

    public static String format(final long value)
    {
        return NumberFormat.getIntegerInstance().format(value);
    }

    /**
     * Build tool held, no screen open.
     */
    @SubscribeEvent
    public void onRenderGui(final RenderGuiEvent.Post event)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui || mc.player == null || !isHoldingBuildTool(mc.player))
        {
            return;
        }
        render(event.getGuiGraphics(), mc);
    }

    /**
     * Build tool window open.
     */
    @SubscribeEvent
    public void onRenderScreen(final ScreenEvent.Render.Post event)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getScreen() instanceof BOScreen screen && screen.getWindow() instanceof WindowExtendedBuildTool)
        {
            render(event.getGuiGraphics(), mc);
        }
    }

    @SubscribeEvent
    public void onLogout(final ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientMinergyData.clear();
    }

    private static boolean isHoldingBuildTool(final LocalPlayer player)
    {
        return player.getMainHandItem().is(ModItems.buildTool.get()) || player.getOffhandItem().is(ModItems.buildTool.get());
    }

    @Nullable
    private static IColony findColony(final Minecraft mc)
    {
        IColony colony = IColonyManager.getInstance().getIColony(mc.player.level(), mc.player.blockPosition());
        if (colony == null)
        {
            colony = IColonyManager.getInstance().getIColonyByOwner(mc.player.level(), mc.player);
        }
        return colony;
    }

    private static void render(final GuiGraphics graphics, final Minecraft mc)
    {
        final IColony colony = findColony(mc);
        if (colony == null)
        {
            return;
        }

        final MinergySyncMessage data = ClientMinergyData.get(colony.getDimension(), colony.getID());
        final Font font = mc.font;

        final Component line1 = data == null
                                  ? Component.translatable("com.minergy.hud.amount", "?")
                                  : Component.translatable("com.minergy.hud.amount", format(data.amount()));
        final Component line2;
        if (data == null)
        {
            line2 = Component.translatable("com.minergy.hud.nodata").withStyle(ChatFormatting.GRAY);
        }
        else if (!data.constructionEnabled())
        {
            line2 = Component.translatable("com.minergy.hud.disabled").withStyle(ChatFormatting.GRAY);
        }
        else
        {
            line2 = Component.translatable(data.minergyFirst() ? "com.minergy.hud.mode.first" : "com.minergy.hud.mode.fallback").withStyle(ChatFormatting.GRAY);
        }

        final int textX = MARGIN + ICON_SIZE + 4;
        final int width = Math.max(font.width(line1), font.width(line2));
        graphics.fill(MARGIN - 2, MARGIN - 2, textX + width + 2, MARGIN + ICON_SIZE + 4, 0x90000000);
        graphics.blit(ICON, MARGIN, MARGIN, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        graphics.drawString(font, line1, textX, MARGIN, COLOR);
        graphics.drawString(font, line2, textX, MARGIN + 10, 0xFFFFFFFF);
    }
}
