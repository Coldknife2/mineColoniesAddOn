package com.coldknife.minergy.ai;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;

/**
 * AI states specific to the Mystical Mage.
 */
public enum MageState implements IAIState
{
    MEDITATE(true);

    private final boolean isOkayToEat;

    MageState(final boolean isOkayToEat)
    {
        this.isOkayToEat = isOkayToEat;
    }

    @Override
    public boolean isOkayToEat()
    {
        return isOkayToEat;
    }
}
