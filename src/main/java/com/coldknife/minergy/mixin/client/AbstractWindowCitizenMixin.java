package com.coldknife.minergy.mixin.client;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.client.CitizenBoostWindow;
import com.ldtteam.blockui.Pane;
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

    /**
     * Vertical step between two tabs of the citizen window, and the slot of the job tab (see minecolonies gui/citizen/nav.xml).
     */
    private static final int TAB_STEP    = 26;
    private static final int JOB_TAB_Y   = 170;

    @Shadow
    @Final
    protected ICitizenDataView citizen;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void minergy$addBoostTab(final ICitizenDataView citizen, final ResourceLocation ui, final CallbackInfo ci)
    {
        final AbstractWindowSkeleton window = (AbstractWindowSkeleton) (Object) this;

        // Right under the last visible tab: the job tab and the debug tab are only shown for some citizens / players.
        int tabY = JOB_TAB_Y;
        if (isVisible(window, "debugTab"))
        {
            tabY = JOB_TAB_Y + 2 * TAB_STEP;
        }
        else if (isVisible(window, "jobTab"))
        {
            tabY = JOB_TAB_Y + TAB_STEP;
        }

        final ButtonImage tab = new ButtonImage();
        tab.setID(BOOST_TAB);
        tab.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/modules/tab_left_side1.png"));
        tab.setSize(32, 26);
        tab.setPosition(0, tabY);

        final ButtonImage icon = new ButtonImage();
        icon.setID(BOOST_ICON);
        icon.setImage(Minergy.id("textures/gui/minergy_boost_tab.png"));
        icon.setSize(20, 20);
        icon.setPosition(5, tabY + 3);

        // Index 0: drawn under the citizen paper like the other tabs.
        window.addChild(icon, 0);
        window.addChild(tab, 0);

        window.registerButton(BOOST_TAB, () -> new CitizenBoostWindow(this.citizen).open());
        window.registerButton(BOOST_ICON, () -> new CitizenBoostWindow(this.citizen).open());
    }

    private static boolean isVisible(final AbstractWindowSkeleton window, final String id)
    {
        final Pane pane = window.findPaneByID(id);
        return pane != null && pane.isVisible();
    }
}
