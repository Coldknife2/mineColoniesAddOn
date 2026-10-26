package com.coldknife.minergy.client;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.core.MinergyConfig;
import com.coldknife.minergy.network.BuyBoostMessage;
import com.coldknife.minergy.network.MinergyNetwork;
import com.coldknife.minergy.network.MinergySyncMessage;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Window of the Boost tab: choose the boost level, then boost a citizen of the colony.
 */
public class MinergyBoostModuleWindow extends AbstractModuleWindow<MinergyBoostModuleView>
{
    private static final String BUTTON_MINUS   = "minus";
    private static final String BUTTON_PLUS    = "plus";
    private static final String BUTTON_BOOST   = "boost";
    private static final String LABEL_LEVEL    = "level";
    private static final String LABEL_COST     = "cost";
    private static final String LABEL_BALANCE  = "balance";
    private static final String LIST_CITIZENS  = "citizens";
    private static final String ROW_NAME       = "name";
    private static final String ROW_STATUS     = "status";

    private static final int REFRESH_TICKS = 20;

    private final ScrollingList        citizenList;
    private final List<ICitizenDataView> citizens = new ArrayList<>();

    private int steps = 1;
    private int tick  = 0;

    public MinergyBoostModuleWindow(final MinergyBoostModuleView moduleView)
    {
        super(moduleView, Minergy.id("gui/layoutminergyboost.xml"));
        citizenList = findPaneOfTypeByID(LIST_CITIZENS, ScrollingList.class);
        registerButton(BUTTON_MINUS, () -> changeSteps(-1));
        registerButton(BUTTON_PLUS, () -> changeSteps(1));
        registerButton(BUTTON_BOOST, this::boostClicked);
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        loadCitizens();
        refreshHeader();
        citizenList.setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return citizens.size();
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ICitizenDataView citizen = citizens.get(index);
                rowPane.findPaneOfTypeByID(ROW_NAME, Text.class).setText(Component.literal(citizen.getName()));
                rowPane.findPaneOfTypeByID(ROW_STATUS, Text.class).setText(statusOf(citizen));
            }
        });
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (++tick >= REFRESH_TICKS)
        {
            tick = 0;
            refreshHeader();
            citizenList.refreshElementPanes();
        }
    }

    private void loadCitizens()
    {
        citizens.clear();
        citizens.addAll(buildingView.getColony().getCitizens().values());
        // Builders first: the boost is mostly meant to speed up constructions.
        citizens.sort(Comparator.comparing((ICitizenDataView c) -> !c.getJob().contains("builder")).thenComparing(ICitizenDataView::getName));
    }

    private void changeSteps(final int delta)
    {
        steps = Math.max(1, Math.min(MinergyConfig.BOOST_MAX_STEPS.get(), steps + delta));
        refreshHeader();
    }

    private void refreshHeader()
    {
        final int bonus = (int) Math.round(steps * MinergyConfig.BOOST_BONUS_PER_STEP.get() * 100);
        final long cost = (long) steps * MinergyConfig.BOOST_COST_STEP.get();
        findPaneOfTypeByID(LABEL_LEVEL, Text.class).setText(Component.translatable("com.minergy.gui.boost.level", bonus, MinergyConfig.BOOST_DURATION_SECONDS.get()));
        findPaneOfTypeByID(LABEL_COST, Text.class).setText(Component.translatable("com.minergy.gui.boost.cost", MinergyHud.format(cost)));

        final MinergySyncMessage data = data();
        findPaneOfTypeByID(LABEL_BALANCE, Text.class).setText(data == null
                                                                 ? Component.translatable("com.minergy.gui.info.nodata")
                                                                 : Component.translatable("com.minergy.gui.boost.balance", MinergyHud.format(data.amount())));
    }

    private Component statusOf(final ICitizenDataView citizen)
    {
        final MinergySyncMessage data = data();
        if (data != null)
        {
            for (final MinergySyncMessage.BoostInfo boost : data.boosts())
            {
                if (boost.citizenId() == citizen.getId())
                {
                    return Component.translatable("com.minergy.gui.boost.active", boost.bonusPercent(), boost.secondsLeft());
                }
            }
        }
        return citizen.getJobComponent();
    }

    private void boostClicked(final Button button)
    {
        final int row = citizenList.getListElementIndexByPane(button);
        if (row < 0 || row >= citizens.size())
        {
            return;
        }
        MinergyNetwork.sendToServer(new BuyBoostMessage(buildingView.getColony().getDimension(),
            buildingView.getColony().getID(),
            citizens.get(row).getId(),
            steps));
    }

    private MinergySyncMessage data()
    {
        return ClientMinergyData.get(buildingView.getColony().getDimension(), buildingView.getColony().getID());
    }
}
