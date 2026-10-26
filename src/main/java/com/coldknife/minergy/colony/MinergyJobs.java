package com.coldknife.minergy.colony;

import com.coldknife.minergy.Minergy;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Jobs added by the add-on to the MineColonies job registry.
 */
public final class MinergyJobs
{
    public static final ResourceLocation MYSTICAL_MAGE_ID = new ResourceLocation(Minergy.MOD_ID, "mysticalmage");

    public static final DeferredRegister<JobEntry> JOBS = DeferredRegister.create(new ResourceLocation("minecolonies", "jobs"), Minergy.MOD_ID);

    public static final RegistryObject<JobEntry> MYSTICAL_MAGE = JOBS.register(MYSTICAL_MAGE_ID.getPath(), () -> new JobEntry.Builder()
        .setJobProducer(JobMysticalMage::new)
        .setJobViewProducer(() -> DefaultJobView::new)
        .setRegistryName(MYSTICAL_MAGE_ID)
        .createJobEntry());

    private MinergyJobs()
    {
    }
}
