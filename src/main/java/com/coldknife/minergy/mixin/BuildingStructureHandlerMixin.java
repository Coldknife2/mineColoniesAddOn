package com.coldknife.minergy.mixin;

import com.coldknife.minergy.core.MinergyConstruction;
import com.coldknife.minergy.marks.MinergyMarks;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Lets the builder place a block paid with Minergy when its item is missing (or before using it, in "Minergy first" mode).
 * <p>
 * Structurize calls, for each block: hasRequiredItems -> placement -> triggerSuccess -> consume.
 */
@Mixin(value = BuildingStructureHandler.class, remap = false)
public abstract class BuildingStructureHandlerMixin
{
    @Shadow
    @Final
    private AbstractEntityAIStructure<?, ?> structureAI;

    /**
     * The current placement was accepted thanks to Minergy and is not paid yet.
     */
    @Unique
    private boolean minergy$pending = false;

    /**
     * The current placement was paid with Minergy: the inventory must not be touched.
     */
    @Unique
    private boolean minergy$skipConsume = false;

    @Inject(method = "hasRequiredItems", at = @At("HEAD"), cancellable = true)
    private void minergy$minergyFirst(final List<ItemStack> requiredItems, final CallbackInfoReturnable<Boolean> cir)
    {
        minergy$pending = false;
        minergy$skipConsume = false;
        if (MinergyConstruction.shouldUseMinergyFirst(structureAI, requiredItems))
        {
            minergy$pending = true;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasRequiredItems", at = @At("RETURN"), cancellable = true)
    private void minergy$minergyFallback(final List<ItemStack> requiredItems, final CallbackInfoReturnable<Boolean> cir)
    {
        if (!minergy$pending && !cir.getReturnValueZ() && MinergyConstruction.canUseMinergyAsFallback(structureAI, requiredItems))
        {
            minergy$pending = true;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "triggerSuccess", at = @At("HEAD"))
    private void minergy$onPlaced(final BlockPos pos, final List<ItemStack> list, final boolean placement, final CallbackInfo ci)
    {
        if (!placement)
        {
            return;
        }

        final IStructureHandler handler = (IStructureHandler) this;
        final BlockPos worldPos = handler.getProgressPosInWorld(pos);
        if (minergy$pending)
        {
            minergy$pending = false;
            if (MinergyConstruction.pay(structureAI))
            {
                minergy$skipConsume = true;
                MinergyMarks.markPlaced(handler.getWorld(), worldPos);
            }
        }
        else if (!list.isEmpty())
        {
            // A real block was placed (with its items): any old Minergy mark there is obsolete.
            MinergyMarks.unmarkPlaced(handler.getWorld(), worldPos);
        }
    }

    @Inject(method = "consume", at = @At("HEAD"), cancellable = true)
    private void minergy$skipConsume(final List<ItemStack> requiredItems, final CallbackInfo ci)
    {
        if (minergy$skipConsume)
        {
            minergy$skipConsume = false;
            ci.cancel();
        }
    }
}
