package com.leclowndu93150.refined_equivalence.datagen;

import com.leclowndu93150.refined_equivalence.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.EMC_LINK.get())
                .pattern("PTP")
                .pattern("RCR")
                .pattern("PTP")
                .define('P', getItem("refinedstorage:advanced_processor"))
                .define('T', getItem("projecte:transmutation_table"))
                .define('R', getItem("projecte:red_matter"))
                .define('C', getItem("refinedstorage:machine_casing"))
                .unlockedBy("has_transmutation_table", has(getItem("projecte:transmutation_table")))
                .save(output);
    }

    private static Item getItem(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
