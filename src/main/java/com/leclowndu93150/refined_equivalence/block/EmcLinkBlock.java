package com.leclowndu93150.refined_equivalence.block;

import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;
import com.leclowndu93150.refined_equivalence.registry.ModBlockEntities;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EmcLinkBlock extends BaseEntityBlock {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public EmcLinkBlock(final Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(CONNECTED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(EmcLinkBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new EmcLinkBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EmcLinkBlockEntity link) {
                link.bindOwner(player);
                link.requestInitialization("placed");
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final BlockHitResult hit
    ) {
        return interact(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        final ItemStack stack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hit
    ) {
        final InteractionResult result = interact(level, pos, player);
        if (result == InteractionResult.PASS) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return result.consumesAction()
            ? ItemInteractionResult.sidedSuccess(level.isClientSide())
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void neighborChanged(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final net.minecraft.world.level.block.Block blockIn,
        final BlockPos fromPos,
        final boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, blockIn, fromPos, isMoving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EmcLinkBlockEntity link) {
            link.onNeighborChanged();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.EMC_LINK.get(), EmcLinkBlockEntity::serverTick);
    }

    private InteractionResult interact(final Level level, final BlockPos pos, final Player player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EmcLinkBlockEntity link)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching()) {
            link.bindOwner(player);
            player.displayClientMessage(
                Component.translatable("message.refined_equivalence.emc_link.bound", player.getName()),
                true
            );
            return InteractionResult.SUCCESS;
        }
        player.openMenu(link, pos);
        return InteractionResult.SUCCESS;
    }
}
