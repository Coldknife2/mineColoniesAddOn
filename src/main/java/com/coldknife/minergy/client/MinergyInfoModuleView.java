package com.coldknife.minergy.client;

import com.coldknife.minergy.Minergy;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Client-only tab of the Mystical Site showing the colony's Minergy (data comes from {@link ClientMinergyData}).
 */
public class MinergyInfoModuleView extends AbstractBuildingModuleView
{
    @Override
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BOWindow getWindow()
    {
        return new MinergyInfoModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Minergy.MOD_ID, "textures/gui/minergy_tab.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.minergy.gui.info.tab");
    }
}
