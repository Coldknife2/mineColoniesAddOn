package com.coldknife.minergy.colony;

import com.coldknife.minergy.ai.EntityAIWorkMysticalMage;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * The Mystical Mage works at the Library and generates Minergy for the colony.
 */
public class JobMysticalMage extends AbstractJob<EntityAIWorkMysticalMage, JobMysticalMage>
{
    public JobMysticalMage(final ICitizenData entity)
    {
        super(entity);
    }

    /**
     * Same look as the university researcher.
     */
    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.STUDENT_ID;
    }

    @NotNull
    @Override
    public EntityAIWorkMysticalMage generateAI()
    {
        return new EntityAIWorkMysticalMage(this);
    }
}
