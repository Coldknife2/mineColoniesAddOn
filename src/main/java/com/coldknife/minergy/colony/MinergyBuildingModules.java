package com.coldknife.minergy.colony;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.client.MinergyBoostModuleView;
import com.coldknife.minergy.client.MinergyInfoModuleView;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.modules.settings.ISettingKey;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.colony.buildings.modules.SettingsModule;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.modules.settings.BoolSetting;
import com.minecolonies.core.colony.buildings.modules.settings.IntSetting;
import com.minecolonies.core.colony.buildings.modules.settings.SettingKey;
import com.minecolonies.core.colony.buildings.modules.settings.StringSetting;
import com.minecolonies.core.colony.buildings.moduleviews.SettingsModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import net.minecraft.resources.ResourceLocation;

/**
 * Modules grafted onto the existing MineColonies Library, which needs no research.
 */
public final class MinergyBuildingModules
{
    public static final String MODE_FALLBACK = "com.minergy.setting.mode.fallback";
    public static final String MODE_FIRST    = "com.minergy.setting.mode.first";

    /**
     * Use Minergy for the builders' constructions.
     */
    public static final ISettingKey<BoolSetting> CONSTRUCTION =
        new SettingKey<>(BoolSetting.class, Minergy.id("construction"));

    /**
     * Fallback (real blocks first) or first (Minergy first).
     */
    public static final ISettingKey<StringSetting> CONSTRUCTION_MODE =
        new SettingKey<>(StringSetting.class, Minergy.id("constructionmode"));

    /**
     * Minergy stock above which the mage stops meditating and walks around to boost the workers (0 = never).
     */
    public static final ISettingKey<IntSetting> AUTO_BOOST_THRESHOLD =
        new SettingKey<>(IntSetting.class, Minergy.id("autoboostthreshold"));

    /**
     * Boost steps given by the mage to each worker (1 step = +50% for 60 s with the default config).
     */
    public static final ISettingKey<IntSetting> AUTO_BOOST_STEPS =
        new SettingKey<>(IntSetting.class, Minergy.id("autobooststeps"));

    public static final BuildingEntry.ModuleProducer<WorkerBuildingModule, WorkerBuildingModuleView> MAGE_WORK =
        new BuildingEntry.ModuleProducer<>("minergy_mage_work",
            () -> new WorkerBuildingModule(MinergyJobs.MYSTICAL_MAGE.get(), Skill.Mana, Skill.Knowledge, true, (building) -> building.getBuildingLevel() > 0 ? 1 : 0),
            () -> WorkerBuildingModuleView::new);

    public static final BuildingEntry.ModuleProducer<SettingsModule, SettingsModuleView> SETTINGS =
        new BuildingEntry.ModuleProducer<>("minergy_settings",
            () -> new SettingsModule()
                .with(CONSTRUCTION, new BoolSetting(true))
                .with(CONSTRUCTION_MODE, new StringSetting(MODE_FALLBACK, MODE_FIRST))
                .with(AUTO_BOOST_THRESHOLD, new IntSetting(5000))
                .with(AUTO_BOOST_STEPS, new IntSetting(1)),
            () -> SettingsModuleView::new);

    public static final BuildingEntry.ModuleProducer<IBuildingModule, MinergyInfoModuleView> INFO =
        new BuildingEntry.ModuleProducer<>("minergy_info", null, () -> MinergyInfoModuleView::new);

    public static final BuildingEntry.ModuleProducer<IBuildingModule, MinergyBoostModuleView> BOOST =
        new BuildingEntry.ModuleProducer<>("minergy_boost", null, () -> MinergyBoostModuleView::new);

    private MinergyBuildingModules()
    {
    }

    /**
     * Adds the add-on modules to the Library entry. Must run before any colony is loaded (common setup).
     * The module list of a {@link BuildingEntry} is mutable and the building registry allows modifications.
     */
    public static void attachToLibrary()
    {
        final BuildingEntry entry = ModBuildings.library.get();
        entry.getModuleProducers().add(INFO);
        entry.getModuleProducers().add(BOOST);
        entry.getModuleProducers().add(MAGE_WORK);
        entry.getModuleProducers().add(SETTINGS);
    }
}
