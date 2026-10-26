package com.coldknife.minergy.core;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side configuration (saved per world in serverconfig/minergy-server.toml).
 */
public final class MinergyConfig
{
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue    COST_PER_BLOCK;
    public static final ForgeConfigSpec.DoubleValue BASE_RATE;
    public static final ForgeConfigSpec.DoubleValue RATE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAX_SKILL_BONUS;
    public static final ForgeConfigSpec.DoubleValue RATE_MULTIPLIER;
    public static final ForgeConfigSpec.LongValue   STORAGE_PER_LEVEL;

    static
    {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Minergy cost of the placements made with Minergy").push("cost");
        COST_PER_BLOCK = builder
            .comment("Minergy consumed by the builder for one block placement without the item in inventory.")
            .defineInRange("costPerBlock", 1, 0, 1_000_000);
        builder.pop();

        builder.comment("Minergy generation by the Mystical Mage, per second of work").push("generation");
        BASE_RATE = builder
            .comment("Minergy per second of a beginner mage at Mystical Site level 1.")
            .defineInRange("baseRate", 4.0D, 0.0D, 1_000_000.0D);
        RATE_PER_LEVEL = builder
            .comment("Additional Minergy per second for each Mystical Site level above 1.")
            .defineInRange("ratePerLevel", 3.0D, 0.0D, 1_000_000.0D);
        MAX_SKILL_BONUS = builder
            .comment("Bonus ratio granted by a mage with Mana 99 (0.25 = +25%). Scales linearly with the Mana skill.")
            .defineInRange("maxSkillBonus", 0.25D, 0.0D, 100.0D);
        RATE_MULTIPLIER = builder
            .comment("Global multiplier applied to the whole generation.")
            .defineInRange("rateMultiplier", 1.0D, 0.0D, 1000.0D);
        builder.pop();

        builder.comment("Minergy storage of a colony").push("storage");
        STORAGE_PER_LEVEL = builder
            .comment("Maximum Minergy a colony can store, per level of its best Mystical Site. 0 = unlimited.")
            .defineInRange("storagePerLevel", 100_000L, 0L, Long.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private MinergyConfig()
    {
    }
}
