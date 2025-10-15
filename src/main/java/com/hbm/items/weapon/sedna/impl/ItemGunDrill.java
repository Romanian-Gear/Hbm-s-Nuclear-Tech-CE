package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class ItemGunDrill extends ItemGunBaseNT implements IFillableItem, IBatteryItem {

    public ItemGunDrill(WeaponQuality quality, String s, GunConfig... cfg) {
        super(quality, s, cfg);
    }

    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return XFactoryDrill.getModdableHarvestLevel(stack, ToolMaterial.IRON.getHarvestLevel());
    }

    public boolean canHarvestBlock(Block par1Block, ItemStack itemStack) {
        return true; // this lets us break things that have no set harvest level (i.e. most NTM shit)
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
            int toFill = Math.min(amount, 50);
            toFill = Math.min(toFill, engine.getCapacity(stack) - this.getFill(stack));
            engine.setAmount(stack, this.getFill(stack) + toFill);
            return amount - toFill;
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

    // TBI
    // Still waiting for bobcat to finish this - SilentYeti
    @Override public void chargeBattery(ItemStack stack, long i) { }
    @Override public void setCharge(ItemStack stack, long i) { }
    @Override public void dischargeBattery(ItemStack stack, long i) { }
    @Override public long getCharge(ItemStack stack) { return 0; }
    @Override public long getMaxCharge(ItemStack stack) { return 0; }
    @Override public long getChargeRate() { return 0; }
    @Override public long getDischargeRate() { return 0; }
}

