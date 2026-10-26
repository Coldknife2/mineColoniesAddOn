package com.coldknife.minergy.loot;

import com.coldknife.minergy.Minergy;
import com.coldknife.minergy.marks.MinergyMarks;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Empties the drops of any block that was placed with Minergy (players, explosions, pistons, ...).
 */
public class NoDropLootModifier extends LootModifier
{
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Minergy.MOD_ID);

    public static final Supplier<Codec<NoDropLootModifier>> CODEC = Suppliers.memoize(
        () -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, NoDropLootModifier::new)));

    public static final RegistryObject<Codec<NoDropLootModifier>> NO_DROP = SERIALIZERS.register("no_drop", CODEC);

    public NoDropLootModifier(final LootItemCondition[] conditions)
    {
        super(conditions);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> generatedLoot, final LootContext context)
    {
        // Only block loot has a block state: entity or chest loot at the same position is untouched.
        final BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        final Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (state == null || origin == null)
        {
            return generatedLoot;
        }

        if (MinergyMarks.isNoDrop(context.getLevel(), BlockPos.containing(origin), state))
        {
            return new ObjectArrayList<>();
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec()
    {
        return CODEC.get();
    }
}
