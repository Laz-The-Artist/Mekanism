package mekanism.common.block.basic;

import javax.annotation.Nonnull;
import mekanism.api.Coord4D;
import mekanism.api.energy.IEnergizedItem;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.block.BlockTileDrops;
import mekanism.common.block.interfaces.IBlockActiveTextured;
import mekanism.common.block.interfaces.IBlockDescriptive;
import mekanism.common.block.interfaces.IHasModel;
import mekanism.common.block.interfaces.IRotatableBlock;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateActive;
import mekanism.common.block.states.IStateFacing;
import mekanism.common.multiblock.IMultiblock;
import mekanism.common.multiblock.IStructuralMultiblock;
import mekanism.common.tile.TileEntitySecurityDesk;
import mekanism.common.tile.TileEntityThermalEvaporationController;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockThermalEvaporationController extends BlockTileDrops implements IBlockActiveTextured, IRotatableBlock, IBlockDescriptive, IHasModel, IStateFacing,
      IStateActive {

    private final String name;

    public BlockThermalEvaporationController() {
        super(Material.IRON);
        setHardness(5F);
        setResistance(10F);
        setCreativeTab(Mekanism.tabMekanism);
        this.name = "thermal_evaporation_controller";
        setTranslationKey(this.name);
        setRegistryName(new ResourceLocation(Mekanism.MODID, this.name));
    }

    @Nonnull
    @Override
    public BlockStateContainer createBlockState() {
        return BlockStateHelper.getBlockState(this);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        //TODO
        return 0;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tile = MekanismUtils.getTileEntitySafe(worldIn, pos);
        if (tile instanceof TileEntityBasicBlock && ((TileEntityBasicBlock) tile).facing != null) {
            state = state.withProperty(BlockStateHelper.facingProperty, ((TileEntityBasicBlock) tile).facing);
        }
        if (tile instanceof IActiveState) {
            state = state.withProperty(BlockStateHelper.activeProperty, ((IActiveState) tile).getActive());
        }
        return state;
    }

    @Override
    @Deprecated
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos fromPos) {
        if (!world.isRemote) {
            TileEntity tileEntity = new Coord4D(pos, world).getTileEntity(world);
            if (tileEntity instanceof IMultiblock) {
                ((IMultiblock<?>) tileEntity).doUpdate();
            }
            if (tileEntity instanceof TileEntityBasicBlock) {
                ((TileEntityBasicBlock) tileEntity).onNeighborChange(neighborBlock);
            }
            if (tileEntity instanceof IStructuralMultiblock) {
                ((IStructuralMultiblock) tileEntity).doUpdate();
            }
        }
    }

    @Override
    @Deprecated
    public boolean isSideSolid(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {
        //TODO: Figure out if this short circuit is good
        return true;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tileEntity = MekanismUtils.getTileEntitySafe(world, pos);
        if (tileEntity instanceof IActiveState) {
            if (((IActiveState) tileEntity).getActive() && ((IActiveState) tileEntity).lightUpdate()) {
                return 15;
            }
        }
        return super.getLightValue(state, world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBasicBlock) {
            TileEntityBasicBlock tileEntity = (TileEntityBasicBlock) te;
            EnumFacing change = EnumFacing.SOUTH;
            if (tileEntity.canSetFacing(EnumFacing.DOWN) && tileEntity.canSetFacing(EnumFacing.UP)) {
                int height = Math.round(placer.rotationPitch);
                if (height >= 65) {
                    change = EnumFacing.UP;
                } else if (height <= -65) {
                    change = EnumFacing.DOWN;
                }
            }
            if (change != EnumFacing.DOWN && change != EnumFacing.UP) {
                int side = MathHelper.floor((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
                switch (side) {
                    case 0:
                        change = EnumFacing.NORTH;
                        break;
                    case 1:
                        change = EnumFacing.EAST;
                        break;
                    case 2:
                        change = EnumFacing.SOUTH;
                        break;
                    case 3:
                        change = EnumFacing.WEST;
                        break;
                }
            }
            tileEntity.setFacing(change);
            tileEntity.redstone = world.getRedstonePowerFromNeighbors(pos) > 0;

            if (tileEntity instanceof TileEntitySecurityDesk) {
                ((TileEntitySecurityDesk) tileEntity).ownerUUID = placer.getUniqueID();
            }
            if (tileEntity instanceof IBoundingBlock) {
                ((IBoundingBlock) tileEntity).onPlace();
            }
        }

        world.markBlockRangeForRenderUpdate(pos, pos.add(1, 1, 1));
        world.checkLightFor(EnumSkyBlock.BLOCK, pos);
        world.checkLightFor(EnumSkyBlock.SKY, pos);

        if (!world.isRemote && te != null) {
            if (te instanceof IMultiblock) {
                ((IMultiblock<?>) te).doUpdate();
            }
            if (te instanceof IStructuralMultiblock) {
                ((IStructuralMultiblock) te).doUpdate();
            }
        }
    }

    @Nonnull
    @Override
    protected ItemStack getDropItem(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        ItemStack ret = new ItemStack(this);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof IStrictEnergyStorage) {
            //This can probably be moved upwards
            IEnergizedItem energizedItem = (IEnergizedItem) ret.getItem();
            energizedItem.setEnergy(ret, ((IStrictEnergyStorage) tileEntity).getEnergy());
        }
        return ret;
    }

    @Override
    public EnumFacing[] getValidRotations(World world, @Nonnull BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        EnumFacing[] valid = new EnumFacing[6];
        if (tile instanceof TileEntityBasicBlock) {
            TileEntityBasicBlock basicTile = (TileEntityBasicBlock) tile;
            for (EnumFacing dir : EnumFacing.VALUES) {
                if (basicTile.canSetFacing(dir)) {
                    valid[dir.ordinal()] = dir;
                }
            }
        }
        return valid;
    }

    @Override
    public boolean rotateBlock(World world, @Nonnull BlockPos pos, @Nonnull EnumFacing axis) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityBasicBlock) {
            TileEntityBasicBlock basicTile = (TileEntityBasicBlock) tile;
            if (basicTile.canSetFacing(axis)) {
                basicTile.setFacing(axis);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        //TODO: Should name just be gotten from registry name
        return LangUtils.localize("tooltip.mekanism." + this.name);
    }

    @Override
    public boolean canRotateTo(EnumFacing side) {
        return Plane.HORIZONTAL.test(side);
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return 9F;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new TileEntityThermalEvaporationController();
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!entityplayer.isSneaking()) {
            if (!world.isRemote) {
                entityplayer.openGui(Mekanism.instance, 33, world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }
}