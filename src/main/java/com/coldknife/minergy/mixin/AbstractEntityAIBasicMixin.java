package com.coldknife.minergy.mixin;

import com.coldknife.minergy.core.MinergyBoosts;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A boosted citizen works faster: every delay of its work AI (block placement and breaking, crafting, ...) is divided
 * by (1 + bonus). With +100%, a builder places its blocks twice as fast.
 */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin<J extends IJob<?>> extends AbstractAISkeleton<J>
{
    protected AbstractEntityAIBasicMixin(final J job)
    {
        super(job);
    }

    @ModifyVariable(method = "setDelay", at = @At("HEAD"), argsOnly = true)
    private int minergy$boostDelay(final int timeout)
    {
        final double bonus = MinergyBoosts.getBonus(worker);
        return bonus > 0 && timeout > 0 ? (int) Math.ceil(timeout / (1.0D + bonus)) : timeout;
    }
}
