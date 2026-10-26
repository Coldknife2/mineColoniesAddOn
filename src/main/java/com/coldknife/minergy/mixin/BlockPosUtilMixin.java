package com.coldknife.minergy.mixin;

import com.coldknife.minergy.marks.MinergyMarks;
import com.minecolonies.api.util.BlockPosUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Citizens (builder deconstructing or upgrading, miners, ...) compute their drops with the citizen position as loot origin,
 * which the global loot modifier cannot match with a block: blocks placed with Minergy are handled here instead.
 */
@Mixin(value = BlockPosUtil.class, remap = false)
public abstract class BlockPosUtilMixin
{
    @Inject(method = "getBlockDrops", at = @At("HEAD"), cancellable = true)
    private static void minergy$noDrops(final Level world, final BlockPos coords, final int fortune, final ItemStack stack, final LivingEntity entity,
        final CallbackInfoReturnable<List<ItemStack>> cir)
    {
        if (MinergyMarks.isNoDrop(world, coords, world.getBlockState(coords)))
        {
            cir.setReturnValue(new ArrayList<>());
        }
    }
}
