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
 * Client-only tab of the Library to buy speed boosts for citizens with Minergy.
 */
public class MinergyBoostModuleView extends AbstractBuildingModuleView
{
    @Override
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BOWindow getWindow()
    {
        return new MinergyBoostModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return Minergy.id("textures/gui/minergy_boost_tab.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.minergy.gui.boost.tab");
    }
}
