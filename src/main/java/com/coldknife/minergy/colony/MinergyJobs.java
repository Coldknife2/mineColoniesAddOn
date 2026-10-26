package com.coldknife.minergy.colony;

import com.coldknife.minergy.Minergy;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Jobs added by the add-on to the MineColonies job registry.
 */
public final class MinergyJobs
{
    public static final ResourceLocation MYSTICAL_MAGE_ID = Minergy.id("mysticalmage");

    public static final DeferredRegister<JobEntry> JOBS = DeferredRegister.create(CommonMinecoloniesAPIImpl.JOBS, Minergy.MOD_ID);

    public static final DeferredHolder<JobEntry, JobEntry> MYSTICAL_MAGE = JOBS.register(MYSTICAL_MAGE_ID.getPath(), () -> new JobEntry.Builder()
        .setJobProducer(JobMysticalMage::new)
        .setJobViewProducer(() -> DefaultJobView::new)
        .setRegistryName(MYSTICAL_MAGE_ID)
        .createJobEntry());

    private MinergyJobs()
    {
    }
}
