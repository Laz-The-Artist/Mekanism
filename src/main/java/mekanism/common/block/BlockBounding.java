package mekanism.common.block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.block.IHasTileEntity;
import mekanism.common.Mekanism;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.base.MekanismTileEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BlockBounding extends Block implements IHasTileEntity<TileEntityBoundingBlock> {

    @Nullable
    private static BlockPos getMainBlockPos(IBlockReader world, BlockPos thisPos) {
        TileEntity te = world.getTileEntity(thisPos);
        if (te instanceof TileEntityBoundingBlock && !thisPos.equals(((TileEntityBoundingBlock) te).getMainPos())) {
            return ((TileEntityBoundingBlock) te).getMainPos();
        }
        return null;
    }

    /**
     * Removes the main block if it is not already air.
     */
    private static void removeMainBlock(World world, BlockPos thisPos) {
        BlockPos mainPos = getMainBlockPos(world, thisPos);
        if (mainPos != null) {
            BlockState state = world.getBlockState(mainPos);
            if (!state.getBlock().isAir(state, world, mainPos)) {
                //Set the main block to air, which will invalidate the rest of the bounding blocks
                world.removeBlock(mainPos, false);
            }
        }
    }

    private final boolean advanced;

    public BlockBounding(boolean advanced) {
        //TODO: Replace meta with two blocks one normal and one advanced with a boolean param
        // Or maybe use blockstate
        super(Block.Properties.create(Material.IRON).hardnessAndResistance(3.5F, 8F));
        this.advanced = advanced;
        if (advanced) {
            setRegistryName(new ResourceLocation(Mekanism.MODID, "advanced_bounding_block"));
        } else {
            setRegistryName(new ResourceLocation(Mekanism.MODID, "bounding_block"));
        }
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) {
            return false;
        }
        BlockState state1 = world.getBlockState(mainPos);
        //TODO: Use proper ray trace result, currently is using the one we got but we probably should make one with correct position information
        return state1.getBlock().onBlockActivated(state1, world, mainPos, player, hand, hit);
    }

    @Override
    public void onReplaced(BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        //Remove the main block if a bounding block gets broken by being directly replaced
        removeMainBlock(world, pos);
        super.onReplaced(state, world, pos, newState, isMoving);
        world.removeTileEntity(pos);
    }

    /**
     * {@inheritDoc} Delegate to main {@link Block#getPickBlock(BlockState, RayTraceResult, IBlockReader, BlockPos, PlayerEntity)}.
     */
    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull BlockState state, RayTraceResult target, @Nonnull IBlockReader world, @Nonnull BlockPos pos, PlayerEntity player) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) {
            return ItemStack.EMPTY;
        }
        BlockState state1 = world.getBlockState(mainPos);
        return state1.getBlock().getPickBlock(state1, target, world, mainPos, player);
    }

    /**
     * {@inheritDoc} Keep tile entity in world until after {@link Block#getDrops(NonNullList, IBlockReader, BlockPos, BlockState, int)}. Used together with {@link
     * Block#harvestBlock(World, PlayerEntity, BlockPos, BlockState, TileEntity, ItemStack)}.
     *
     * @author Forge
     * @see FlowerPotBlock#removedByPlayer(BlockState, World, BlockPos, PlayerEntity, boolean, IFluidState)
     */
    @Override
    public boolean removedByPlayer(@Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, boolean willHarvest, IFluidState fluidState) {
        if (willHarvest) {
            return true;
        }
        removeMainBlock(world, pos);
        return super.removedByPlayer(state, world, pos, player, false, fluidState);
    }

    /**
     * {@inheritDoc} Delegate to main {@link Block#getDrops(NonNullList, IBlockReader, BlockPos, BlockState, int)}.
     */
    //TODO: Loot table? Or how should the bounding block handle drops
    /*@Override
    public void getDrops(@Nonnull NonNullList<ItemStack> drops, IBlockReader world, BlockPos pos, @Nonnull BlockState state, int fortune) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) {
            return;
        }
        BlockState state1 = world.getBlockState(mainPos);
        state1.getBlock().getDrops(drops, world, mainPos, state1, fortune);
    }*/

    /**
     * {@inheritDoc} Used together with {@link Block#removedByPlayer(BlockState, World, BlockPos, PlayerEntity, boolean, IFluidState)}.
     *
     * @author Forge
     * @see FlowerPotBlock#harvestBlock(World, PlayerEntity, BlockPos, BlockState, TileEntity, ItemStack)
     */
    @Override
    public void harvestBlock(@Nonnull World world, PlayerEntity player, @Nonnull BlockPos pos, @Nonnull BlockState state, TileEntity te, @Nonnull ItemStack stack) {
        super.harvestBlock(world, player, pos, state, te, stack);
        world.removeBlock(pos, false);
    }

    /**
     * Returns that this "cannot" be silk touched. This is so that {@link Block#getSilkTouchDrop(BlockState)} is not called, because only {@link
     * Block#getDrops(NonNullList, IBlockReader, BlockPos, BlockState, int)} supports tile entities. Our blocks keep their inventory and other behave like they are being
     * silk touched by default anyway.
     *
     * @return false
     */
    //TODO: Silk touch/denial
    /*@Override
    @Deprecated
    protected boolean canSilkHarvest() {
        return false;
    }*/

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock) world.getTileEntity(pos);
        if (tileEntity != null) {
            tileEntity.onNeighborChange(state.getBlock());
        }
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos != null) {
            BlockState state1 = world.getBlockState(mainPos);
            state1.getBlock().neighborChanged(state1, world, mainPos, neighborBlock, neighborPos, isMoving);
        }
    }

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(BlockState state, @Nonnull PlayerEntity player, @Nonnull IBlockReader world, @Nonnull BlockPos pos) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) {
            return super.getPlayerRelativeBlockHardness(state, player, world, pos);
        }
        BlockState state1 = world.getBlockState(mainPos);
        return state1.getBlock().getPlayerRelativeBlockHardness(state1, player, world, mainPos);
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public TileEntityType<TileEntityBoundingBlock> getTileType() {
        if (advanced) {
            return MekanismTileEntityTypes.ADVANCED_BOUNDING_BLOCK;
        }
        return MekanismTileEntityTypes.BOUNDING_BLOCK;
    }
}