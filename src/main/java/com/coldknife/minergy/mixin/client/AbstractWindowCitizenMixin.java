package com.coldknife.minergy.mixin.client;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.client.CitizenBoostWindow;
import com.ldtteam.blockui.controls.ButtonImage;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Minergy boost" tab under the other tabs of every citizen window (right click on a citizen).
 */
@Mixin(value = AbstractWindowCitizen.class, remap = false)
public abstract class AbstractWindowCitizenMixin
{
    private static final String BOOST_TAB  = "minergyBoostTab";
    private static final String BOOST_ICON = "minergyBoostIcon";

    @Shadow
    @Final
    protected ICitizenDataView citizen;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void minergy$addBoostTab(final ICitizenDataView citizen, final ResourceLocation ui, final CallbackInfo ci)
    {
        final AbstractWindowSkeleton window = (AbstractWindowSkeleton) (Object) this;

        final ButtonImage tab = new ButtonImage();
        tab.setID(BOOST_TAB);
        tab.setImage(new ResourceLocation("minecolonies", "textures/gui/modules/tab_left_side1.png"), false);
        tab.setSize(32, 26);
        tab.setPosition(0, 222);

        final ButtonImage icon = new ButtonImage();
        icon.setID(BOOST_ICON);
        icon.setImage(Minergy.id("textures/gui/minergy_boost_tab.png"), false);
        icon.setSize(20, 20);
        icon.setPosition(5, 225);

        // Index 0: drawn under the citizen paper like the other tabs.
        window.addChild(icon, 0);
        window.addChild(tab, 0);

        window.registerButton(BOOST_TAB, () -> new CitizenBoostWindow(this.citizen).open());
        window.registerButton(BOOST_ICON, () -> new CitizenBoostWindow(this.citizen).open());
    }
}
