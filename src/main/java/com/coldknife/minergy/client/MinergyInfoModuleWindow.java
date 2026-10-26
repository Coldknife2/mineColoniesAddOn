package com.coldknife.minergy.client;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.network.MinergySyncMessage;
import com.ldtteam.blockui.controls.Text;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Window of the Minergy tab of the Mystical Site.
 */
public class MinergyInfoModuleWindow extends AbstractModuleWindow<MinergyInfoModuleView>
{
    private static final String LABEL_AMOUNT       = "amount";
    private static final String LABEL_RATE         = "rate";
    private static final String LABEL_CONSTRUCTION = "construction";
    private static final String LABEL_MODE         = "mode";

    public MinergyInfoModuleWindow(final MinergyInfoModuleView moduleView)
    {
        super(moduleView, new ResourceLocation(Minergy.MOD_ID, "gui/layoutminergyinfo.xml"));
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        refresh();
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        refresh();
    }

    private void refresh()
    {
        final MinergySyncMessage data = ClientMinergyData.get(buildingView.getColony().getDimension(), buildingView.getColony().getID());
        if (data == null)
        {
            setText(LABEL_AMOUNT, Component.translatable("com.minergy.gui.info.nodata"));
            setText(LABEL_RATE, Component.empty());
            setText(LABEL_CONSTRUCTION, Component.empty());
            setText(LABEL_MODE, Component.empty());
            return;
        }

        setText(LABEL_AMOUNT, data.capacity() == Long.MAX_VALUE
                                ? Component.translatable("com.minergy.gui.info.amount.unlimited", MinergyHud.format(data.amount()))
                                : Component.translatable("com.minergy.gui.info.amount", MinergyHud.format(data.amount()), MinergyHud.format(data.capacity())));
        setText(LABEL_RATE, Component.translatable("com.minergy.gui.info.rate", String.format("%.1f", data.rate())));
        setText(LABEL_CONSTRUCTION, Component.translatable(data.constructionEnabled() ? "com.minergy.gui.info.construction.on" : "com.minergy.gui.info.construction.off"));
        setText(LABEL_MODE, Component.translatable(data.minergyFirst() ? "com.minergy.gui.info.mode.first" : "com.minergy.gui.info.mode.fallback"));
    }

    private void setText(final String id, final Component text)
    {
        final Text pane = findPaneOfTypeByID(id, Text.class);
        if (pane != null)
        {
            pane.setText(text);
        }
    }
}
