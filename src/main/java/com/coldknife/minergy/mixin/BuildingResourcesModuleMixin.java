package com.coldknife.minergy.mixin;

import com.coldknife.minergy.core.MinergyConstruction;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule;
import com.minecolonies.core.colony.buildings.utils.BuilderBucket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In "Minergy first" mode, the builder does not ask the couriers for resources the Minergy will pay.
 */
@Mixin(value = BuildingResourcesModule.class, remap = false)
public abstract class BuildingResourcesModuleMixin
{
    @Inject(method = "checkOrRequestBucket", at = @At("HEAD"), cancellable = true)
    private void minergy$skipRequest(final BuilderBucket requiredResources, final ICitizenData worker, final CallbackInfo ci)
    {
        if (MinergyConstruction.shouldSkipBucketRequest(((IBuildingModule) this).getBuilding(), requiredResources))
        {
            ci.cancel();
        }
    }
}
