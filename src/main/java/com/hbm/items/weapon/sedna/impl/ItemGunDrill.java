package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineEnergy;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ItemGunDrill extends ItemGunBaseNT implements IFillableItem, IBatteryItem {

    public ItemGunDrill(WeaponQuality quality, String s, GunConfig... cfg) {
        super(quality, s, cfg);
    }
    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState blockState) {
        int defaultLevel = ToolMaterial.IRON.getHarvestLevel();
        return XFactoryDrill.getModdableHarvestLevel(stack, defaultLevel);
    }
    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        return 50.0F; // extremely fast to simulate instant mining
    }
    @Override
    public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
        return true; // this lets us break things that have no set harvest level (i.e. most NTM shit)
    }
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        World world = player.world;
        IBlockState state = world.getBlockState(pos);
        if (!world.isRemote) {
            // Force block drops ignoring harvest checks
            state.getBlock().dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos); // actually remove the block
        }
        return true; // This is what bypasses the system in place on 1.12. Makes it work identical to 1.7. - Yeti
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        return mag instanceof MagazineFluid;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineFluid) {
            MagazineFluid engine = (MagazineFluid) mag;
            for(FluidType acc : engine.acceptedTypes) {
                if(type == acc) {
                    return amount;
                }
            }
        }

        return 0;
    }

    @Override public boolean providesFluid(FluidType type, ItemStack stack) { return false; }
    @Override public int tryEmpty(FluidType type, int amount, ItemStack stack) { return amount; }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        if(mag instanceof MagazineFluid) return ((MagazineFluid) mag).getType(stack, null);
        return Fluids.NONE;
    }

    @Override
    public int getFill(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineFluid) {
            MagazineFluid engine = (MagazineFluid) mag;
            return engine.getAmount(stack, null);
        }

        return 0;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineEnergy) {
            MagazineEnergy engine = (MagazineEnergy) mag;
            engine.setAmount(stack, Math.min(engine.capacity, engine.getAmount(stack, null) + (int) i));
        }
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineEnergy) {
            MagazineEnergy engine = (MagazineEnergy) mag;
            engine.setAmount(stack, (int) i);
        }
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineEnergy) {
            MagazineEnergy engine = (MagazineEnergy) mag;
            engine.setAmount(stack, Math.max(0, engine.getAmount(stack, null) - (int) i));
        }
    }

    @Override
    public long getCharge(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineEnergy) {
            MagazineEnergy engine = (MagazineEnergy) mag;
            return engine.getAmount(stack, null);
        }

        return 0;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag instanceof MagazineEnergy) {
            MagazineEnergy engine = (MagazineEnergy) mag;
            return engine.getCapacity(stack);
        }

        return 0;
    }

    @Override public long getChargeRate() { return 50_000; }
    @Override public long getDischargeRate() { return 0; }
}

