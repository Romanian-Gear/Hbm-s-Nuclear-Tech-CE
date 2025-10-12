package com.hbm.items.weapon.sedna.factory;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mods.WeaponModManager;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.misc.RenderScreenOverlay;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.function.BiConsumer;

public class XFactoryDrill {

    public static final String D_REACH =	"D_REACH";
    public static final String F_DTNEG =	"F_DTNEG";
    public static final String F_PIERCE =	"F_PIERCE";
    public static final String I_AOE =		"I_AOE";
    public static final String I_HARVEST =	"I_HARVEST";

    public static void init() {

        ModItems.gun_drill = new ItemGunDrill(ItemGunBaseNT.WeaponQuality.UTILITY, new GunConfig()
                .dura(3_000).draw(10).inspect(55).hideCrosshair(false).crosshair(RenderScreenOverlay.Crosshair.L_CIRCUMFLEX)
                .rec(new Receiver(0)
                        .dmg(10F).delay(20).auto(true).jam(0)
                        .mag(new MagazineFluid(0, 4_000, Fluids.GASOLINE, Fluids.GASOLINE_LEADED, Fluids.COALGAS, Fluids.COALGAS_LEADED))
                        .offset(1, -0.0625 * 2.5, -0.25D)
                        .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_DRILL_FIRE))
                        .setupStandardFire())
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER);
                //.anim(LAMBDA_CT_ANIMS).orchestra(Orchestras.ORCHESTRA_DRILL)
    }

    public static BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_DRILL_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, true);
    };

    public static void doStandardFire(ItemStack stack, ItemGunBaseNT.LambdaContext ctx, boolean calcWear) {
        EntityPlayer player = ctx.getPlayer();
        if (player == null) return;

        Receiver primary = ctx.config.getReceivers(stack)[0];
        IMagazine mag = primary.getMagazine(stack);

        RayTraceResult mop = EntityDamageUtil.getMouseOver(ctx.getPlayer(), getModdableReach(stack, 5.0D));
        if(mop != null) {
            if(mop.typeOfHit == mop.typeOfHit.ENTITY) {
                float damage = 5.0F;
                if(mop.entityHit instanceof EntityLivingBase) {
                    EntityDamageUtil.attackEntityFromNT((EntityLivingBase) mop.entityHit, DamageSource.causePlayerDamage(ctx.getPlayer()), damage, true, true, 0.1F, getModdableDTNegation(stack, 2F), getModdablePiercing(stack, 0.15F));
                } else {
                    mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(ctx.getPlayer()), damage);
                }
            }
            if(player != null && mop.typeOfHit == mop.typeOfHit.BLOCK) {

                int aoe = getModdableAoE(stack, 1);
                for(int i = -aoe; i <= aoe; i++) {
                    for(int j = -aoe; j <= aoe; j++) {
                        for(int k = -aoe; k <= aoe; k++) {
                            BlockPos targetPos = mop.getBlockPos().add(i, j, k);
                            breakExtraBlock(player.world, targetPos, player, mop.getBlockPos());
                        }
                    }
                }
            }
        }

        mag.useUpAmmo(stack, ctx.inventory, 10);
        if (calcWear) ItemGunBaseNT.setWear(stack, ctx.configIndex, Math.min(ItemGunBaseNT.getWear(stack, ctx.configIndex), ctx.config.getDurability(stack)));
    }

    public static void breakExtraBlock(World world, BlockPos pos, EntityPlayer playerEntity, BlockPos refPos) {
        if (world.isAirBlock(pos) || !(playerEntity instanceof EntityPlayerMP player)) return;

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!block.canHarvestBlock(world, pos, player)
                || block.getBlockHardness(state, world, pos) == -1.0F
                || block.getBlockHardness(state, world, pos) == 0.0F)
        {
            world.playSound(null, pos, block.getSoundType(state, world, pos, player).getBreakSound(), block.getSoundType(state, world, pos, player).getVolume(), 0.8F + world.rand.nextFloat() * 0.6F);
            return;
        }

        // we are serverside and tryHarvestBlock already invokes the 2001 packet for every player except the user, so we manually send it for the user as well
        player.interactionManager.tryHarvestBlock(pos);

        if(world.isAirBlock(pos)) { // only do this when the block was destroyed. if the block doesn't create air when broken, this breaks, but it's no big deal
            player.connection.sendPacket(new SPacketEffect(2001, pos, Block.getStateId(state), false));
        }
    }

    // this system technically doesn't need to be part of the GunCfg or Receiver or anything, we can just do this and it works the exact same
    public static double getModdableReach(ItemStack stack, double base) {		return WeaponModManager.eval(base, stack, D_REACH, ModItems.gun_drill, 0); }
    public static float getModdableDTNegation(ItemStack stack, float base) {	return WeaponModManager.eval(base, stack, F_DTNEG, ModItems.gun_drill, 0); }
    public static float getModdablePiercing(ItemStack stack, float base) {		return WeaponModManager.eval(base, stack, F_PIERCE, ModItems.gun_drill, 0); }
    public static int getModdableAoE(ItemStack stack, int base) {				return WeaponModManager.eval(base, stack, I_AOE, ModItems.gun_drill, 0); }
    public static int getModdableHarvestLevel(ItemStack stack, int base) {		return WeaponModManager.eval(base, stack, I_HARVEST, ModItems.gun_drill, 0); }
}
