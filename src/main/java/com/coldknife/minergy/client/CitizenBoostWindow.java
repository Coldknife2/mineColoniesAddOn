package com.coldknife.minergy.client;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.core.MinergyConfig;
import com.coldknife.minergy.network.BuyBoostMessage;
import com.coldknife.minergy.network.MinergyNetwork;
import com.coldknife.minergy.network.MinergySyncMessage;
import com.ldtteam.blockui.controls.Text;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import net.minecraft.network.chat.Component;

/**
 * Boost window of one citizen, opened from the "Minergy boost" tab of the citizen window.
 */
public class CitizenBoostWindow extends AbstractWindowSkeleton
{
    private static final String BUTTON_MINUS  = "minus";
    private static final String BUTTON_PLUS   = "plus";
    private static final String BUTTON_BOOST  = "boost";
    private static final String BUTTON_BACK   = "back";
    private static final String LABEL_NAME    = "name";
    private static final String LABEL_LEVEL   = "level";
    private static final String LABEL_COST    = "cost";
    private static final String LABEL_BALANCE = "balance";
    private static final String LABEL_STATUS  = "status";

    private static final int REFRESH_TICKS = 20;

    private final ICitizenDataView citizen;

    private int steps = 1;
    private int tick  = 0;

    public CitizenBoostWindow(final ICitizenDataView citizen)
    {
        super(Minergy.id("gui/layoutcitizenboost.xml"));
        this.citizen = citizen;
        registerButton(BUTTON_MINUS, () -> changeSteps(-1));
        registerButton(BUTTON_PLUS, () -> changeSteps(1));
        registerButton(BUTTON_BOOST, this::boost);
        registerButton(BUTTON_BACK, () -> new MainWindowCitizen(citizen).open());
        findPaneOfTypeByID(LABEL_NAME, Text.class).setText(Component.literal(citizen.getName()));
        refresh();
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (++tick >= REFRESH_TICKS)
        {
            tick = 0;
            refresh();
        }
    }

    private void changeSteps(final int delta)
    {
        steps = Math.max(1, Math.min(MinergyConfig.BOOST_MAX_STEPS.get(), steps + delta));
        refresh();
    }

    private void boost()
    {
        MinergyNetwork.sendToServer(new BuyBoostMessage(citizen.getColony().getDimension(), citizen.getColony().getID(), citizen.getId(), steps));
    }

    private void refresh()
    {
        final int bonus = (int) Math.round(steps * MinergyConfig.BOOST_BONUS_PER_STEP.get() * 100);
        final long cost = (long) steps * MinergyConfig.BOOST_COST_STEP.get();
        findPaneOfTypeByID(LABEL_LEVEL, Text.class).setText(Component.translatable("com.minergy.gui.boost.level", bonus, MinergyConfig.BOOST_DURATION_SECONDS.get()));
        findPaneOfTypeByID(LABEL_COST, Text.class).setText(Component.translatable("com.minergy.gui.boost.cost", MinergyHud.format(cost)));

        final MinergySyncMessage data = ClientMinergyData.get(citizen.getColony().getDimension(), citizen.getColony().getID());
        findPaneOfTypeByID(LABEL_BALANCE, Text.class).setText(data == null
                                                                 ? Component.translatable("com.minergy.gui.info.nodata")
                                                                 : Component.translatable("com.minergy.gui.boost.balance", MinergyHud.format(data.amount())));
        Component status = Component.translatable("com.minergy.gui.boost.none");
        if (data != null)
        {
            for (final MinergySyncMessage.BoostInfo boost : data.boosts())
            {
                if (boost.citizenId() == citizen.getId())
                {
                    status = Component.translatable("com.minergy.gui.boost.active", boost.bonusPercent(), boost.secondsLeft());
                }
            }
        }
        findPaneOfTypeByID(LABEL_STATUS, Text.class).setText(status);
    }
}
