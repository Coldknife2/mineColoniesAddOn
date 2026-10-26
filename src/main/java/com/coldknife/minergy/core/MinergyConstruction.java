package com.coldknife.minergy.core;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.utils.BuilderBucket;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rules for the builders' placements paid with Minergy.
 */
public final class MinergyConstruction
{
    private MinergyConstruction()
    {
    }

    public static long costPerBlock()
    {
        return MinergyConfig.COST_PER_BLOCK.get();
    }

    /**
     * @return the colony of the AI if it is a builder whose colony allows Minergy for constructions, else null.
     */
    @Nullable
    public static IColony getBuilderColony(final AbstractEntityAIStructure<?, ?> ai)
    {
        if (!(ai instanceof EntityAIStructureBuilder))
        {
            return null;
        }
        final IColony colony = ai.getWorker().getCitizenColonyHandler().getColony();
        if (colony == null || !MinergyManager.isConstructionEnabled(colony))
        {
            return null;
        }
        return colony;
    }

    /**
     * Only real block placements can be paid with Minergy: entities (item frames, armor stands...), fluids (buckets) and
     * fire (flint and steel) keep needing their items, as they could otherwise be picked back up as items.
     */
    public static boolean canBePaidWithMinergy(final List<ItemStack> requiredItems)
    {
        if (requiredItems.isEmpty())
        {
            return false;
        }
        for (final ItemStack stack : requiredItems)
        {
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if this placement must be paid with Minergy: always in "Minergy first" mode, and in fallback mode when the
     * builder does not carry every required item.
     */
    public static boolean shouldUseMinergy(final AbstractEntityAIStructure<?, ?> ai, final List<ItemStack> requiredItems)
    {
        final IColony colony = getBuilderColony(ai);
        if (colony == null || !canBePaidWithMinergy(requiredItems) || !MinergyManager.has(colony, costPerBlock()))
        {
            return false;
        }
        return MinergyManager.isConstructionMinergyFirst(colony) || !hasAllInInventory(ai, requiredItems);
    }

    /**
     * Same check as the builder's inventory lookup, without creating any request.
     */
    private static boolean hasAllInInventory(final AbstractEntityAIStructure<?, ?> ai, final List<ItemStack> requiredItems)
    {
        final Map<ItemStorage, Integer> needed = new HashMap<>();
        for (final ItemStack stack : requiredItems)
        {
            needed.merge(new ItemStorage(stack), stack.getCount(), Integer::sum);
        }
        for (final Map.Entry<ItemStorage, Integer> entry : needed.entrySet())
        {
            final ItemStack wanted = entry.getKey().getItemStack();
            final int count = InventoryUtils.getItemCountInItemHandler(ai.getWorker().getInventoryCitizen(),
                stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(wanted, stack));
            if (count < entry.getValue())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Pays one placement.
     */
    public static boolean pay(final AbstractEntityAIStructure<?, ?> ai)
    {
        final IColony colony = ai.getWorker().getCitizenColonyHandler().getColony();
        return colony != null && MinergyManager.tryConsume(colony, costPerBlock());
    }

    /**
     * In "Minergy first" mode, the builder does not ask the warehouse for a bucket of resources the Minergy can pay entirely.
     */
    public static boolean shouldSkipBucketRequest(final IBuilding building, @Nullable final BuilderBucket bucket)
    {
        if (bucket == null || !(building instanceof BuildingBuilder))
        {
            return false;
        }
        final IColony colony = building.getColony();
        if (!MinergyManager.isConstructionEnabled(colony) || !MinergyManager.isConstructionMinergyFirst(colony))
        {
            return false;
        }
        long total = 0;
        for (final int count : bucket.getResourceMap().values())
        {
            total += count;
        }
        return MinergyManager.has(colony, total * costPerBlock());
    }
}
