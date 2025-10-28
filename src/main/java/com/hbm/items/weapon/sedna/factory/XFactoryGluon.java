package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunGluon;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSource;
import com.hbm.main.MainRegistry;
import com.hbm.main.ModEventHandlerClient;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PacketSpecialDeath;
import com.hbm.particle.gluon.ParticleGluonBurnTrail;
import com.hbm.particle.gluon.ParticleGluonFlare;
import com.hbm.particle.gluon.ParticleGluonMuzzleSmoke;
import com.hbm.particle.tau.ParticleTauParticle;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.IType;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna;
import com.hbm.render.anim.sedna.HbmAnimationsSedna;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.sound.AudioWrapper;
import com.hbm.main.ResourceManager;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class XFactoryGluon {

    public static BulletConfig energy_gluon;
    public static float chargeScaling = 1.011619F; //double dmg every 2 sec
    
    // Track active beam particles for players
    public static Map<EntityPlayer, ParticleGluonBurnTrail> activeTrailParticles = new HashMap<>();
    
    public static void init() {
        
        energy_gluon = new BulletConfig().setItem(GunFactory.EnumAmmo.TAU_URANIUM).setCasing(new ItemStack(ModItems.plate_lead, 2), 16).setDoesPenetrate(false);
        
        ModItems.gun_egon = new ItemGunGluon(ItemGunBaseNT.WeaponQuality.LEGENDARY, "gun_egon", new GunConfig()
                .dura(10_000).draw(10).inspect(33).crosshair(Crosshair.NONE)
                .rec(new Receiver(0)
                        .dmg(8F).delay(1).auto(true).spread(0F).spreadHipfire(0F)
                        .mag(new MagazineBelt().addConfigs(energy_gluon))
                        .offset(0.75, -0.0625, -0.1875D)
                        .canFire(LAMBDA_GLUON_CAN_FIRE).fire(LAMBDA_GLUON_FIRE))
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                .pr(Lego.LAMBDA_STANDARD_RELOAD)
                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                .anim(LAMBDA_GLUON_ANIMS).orchestra(ORCHESTRA_GLUON)
        );
    }
    
    public static float[] getBeamDirectionOffset(float time) {
        float sinval = MathHelper.sin(time * 1.2F) + MathHelper.sin(time * 0.8F - 10) + MathHelper.sin(time * 1.0F + 10);
        sinval /= 3;
        float sinval2 = MathHelper.sin(time * 0.6F) + MathHelper.sin(time * 0.2F + 20) + MathHelper.sin(time * 0.1F + 20);
        sinval2 /= 3;
        return new float[]{BobMathUtil.remap(sinval, -1, 1, -3, 3), BobMathUtil.remap(sinval2, -1, 1, -0.5F, 0.5F)};
    }
    
    public static float[] getBeamVisualOffset(float time) {
        float sinval = MathHelper.sin(time * 0.15F) + MathHelper.sin(time * 0.25F - 10) + MathHelper.sin(time * 0.1F + 10);
        sinval /= 3;
        float sinval2 = MathHelper.sin(time * 0.1F) + MathHelper.sin(time * 0.05F + 20) + MathHelper.sin(time * 0.13F + 20);
        sinval2 /= 3;
        return new float[]{BobMathUtil.remap((float) Library.smoothstep(sinval, -1, 1), 0, 1, -2, 1.5F), BobMathUtil.remap(sinval2, -1, 1, -0.03F, 0.05F)};
    }
    
    public static BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> LAMBDA_GLUON_CAN_FIRE = (stack, ctx) -> {
        return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) >= 1;
    };
    
    public static BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_GLUON_FIRE = (stack, ctx) -> {
        EntityLivingBase entity = ctx.entity;
        EntityPlayer player = ctx.getPlayer();
        int index = ctx.configIndex;
        
        // Play animation on first fire
        if(ItemGunBaseNT.getLastAnim(stack, index) != HbmAnimationsSedna.AnimType.CYCLE) {
            ItemGunBaseNT.playAnimation(player, stack, HbmAnimationsSedna.AnimType.CYCLE, index);
        }
        
        // Server-side damage logic
        if(!entity.world.isRemote) {
            float[] angles = getBeamDirectionOffset(entity.world.getTotalWorldTime() + 1);
            Vec3d look = Library.changeByAngle(entity.getLook(1), angles[0], angles[1]);
            RayTraceResult r = Library.rayTraceIncludeEntitiesCustomDirection(player, look, 50, 1);
            
            if(r != null && r.typeOfHit == Type.ENTITY && r.entityHit instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) r.entityHit;
                if(target instanceof EntityPlayer && ((EntityPlayer) target).isCreative()) {
                    return;
                }
                
                // Get or initialize charge multiplier
                float charge = ItemGunBaseNT.getValueFloat(stack, "gluon_charge");
                if(charge == 0) charge = 0.25F;
                
                charge = charge * chargeScaling;
                ItemGunBaseNT.setValueFloat(stack, "gluon_charge", charge);
                
                float damage = Math.min(target.getHealth(), charge);
                target.getCombatTracker().trackDamage(ModDamageSource.gluon, target.getHealth(), damage);
                target.setHealth(target.getHealth() - damage);
                
                PacketDispatcher.wrapper.sendToAllTracking(new PacketSpecialDeath(target, 1), target);
                if(target instanceof EntityPlayerMP) {
                    PacketDispatcher.wrapper.sendTo(new PacketSpecialDeath(target, 1), (EntityPlayerMP) target);
                }
                
                if(target.getHealth() <= 0) {
                    PacketDispatcher.wrapper.sendToAllTracking(new PacketSpecialDeath(target, 0), target);
                    target.setDead();
                    target.onDeath(ModDamageSource.gluon);
                    target.onKillCommand();
                    
                    if(target instanceof EntityPlayerMP) {
                        PacketDispatcher.wrapper.sendTo(new PacketSpecialDeath(target, 0), (EntityPlayerMP) target);
                    }
                }
            } else {
                ItemGunBaseNT.setValueFloat(stack, "gluon_charge", 0.25F);
            }
        }
        
        // Client-side particles
        if(entity.world.isRemote) {
            spawnGluonParticles(entity, player, stack);
        }
        
        // Consume ammo every 5 ticks
        if(entity.world.getTotalWorldTime() % 5 == 0) {
            ctx.config.getReceivers(stack)[0].getMagazine(stack).useUpAmmo(stack, ctx.inventory, 1);
        }
        
        ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + 0.1F, ctx.config.getDurability(stack)));
    };
    
    @SideOnly(Side.CLIENT)
    private static void spawnGluonParticles(EntityLivingBase entity, EntityPlayer player, ItemStack stack) {
        if(player != Minecraft.getMinecraft().player) return;
        
        float[] angles = getBeamDirectionOffset(player.world.getTotalWorldTime() + 1);
        Vec3d look = Library.changeByAngle(player.getLook(1), angles[0], angles[1]);
        RayTraceResult r = Library.rayTraceIncludeEntitiesCustomDirection(player, look, 50, 1);
        
        if(r != null && r.hitVec != null && r.typeOfHit != Type.MISS && r.sideHit != null) {
            Vec3i norm = r.sideHit.getDirectionVec();
            Vec3d pos = r.hitVec.add(norm.getX() * 0.1F, norm.getY() * 0.1F, norm.getZ() * 0.1F);
            ParticleGluonFlare flare = new ParticleGluonFlare(player.world, pos.x, pos.y, pos.z, player);
            Minecraft.getMinecraft().effectRenderer.addEffect(flare);
        } else {
            Vec3d pos = player.getPositionEyes(1).add(look.scale(50));
            ParticleGluonFlare flare = new ParticleGluonFlare(player.world, pos.x, pos.y, pos.z, player);
            Minecraft.getMinecraft().effectRenderer.addEffect(flare);
        }
        
        Random rand = player.world.rand;
        float partialTicks = MainRegistry.proxy.partialTicks();
        
        for(int i = 0; i < 2; i++) {
            Vec3d randPos = new Vec3d(rand.nextFloat() - 0.5, rand.nextFloat() - 0.5, rand.nextFloat() - 0.5).scale(0.05);
            Vec3d start = player.getPositionEyes(partialTicks).add(look.scale(0.5)).add(randPos);
            ParticleTauParticle p = new ParticleTauParticle(player.world, start.x, start.y, start.z, 0.05F, 0.02F, 1, 3, 0F);
            p.motion((rand.nextFloat() - 0.5F) * 0.04F, (rand.nextFloat() - 0.5F) * 0.04F, (rand.nextFloat() - 0.5F) * 0.04F);
            p.lifetime(6 + rand.nextInt(4));
            p.color(0.2F, 0.4F + player.world.rand.nextFloat() * 0.5F, 1F, 2F);
            Minecraft.getMinecraft().effectRenderer.addEffect(p);
        }
        
        if(Minecraft.getMinecraft().world.getTotalWorldTime() % 2 == 0) {
            ModEventHandlerClient.firstPersonAuxParticles.add(new ParticleGluonMuzzleSmoke(player.world, 0, 0, 4.1, 0, ResourceManager.gluon_muzzle_smoke, 10, 50, 9).color(0.2F, 0.4F + player.world.rand.nextFloat() * 0.5F, 1F, 3F).lifetime(10));
        }
        if(Minecraft.getMinecraft().world.getTotalWorldTime() % 4 == 0) {
            ModEventHandlerClient.firstPersonAuxParticles.add(new ParticleGluonMuzzleSmoke(player.world, 0, 0, 4, 1, ResourceManager.gluon_muzzle_glow, 30, 50, -1).color(0.2F, 0.4F + player.world.rand.nextFloat() * 0.5F, 1F, 2F).lifetime(16));
        }
        
        // Initial fire effect
        int activeTicks = ItemGunBaseNT.getValueInt(stack, "gluon_active_ticks");
        if(activeTicks < 3) {
            for(int i = 0; i < 3; i++)
                ModEventHandlerClient.firstPersonAuxParticles.add(new ParticleGluonMuzzleSmoke(player.world, 0, 0, 4.1, 0, ResourceManager.gluon_muzzle_smoke, 10, 50, 25).color(0.2F, 0.4F, 1F, 3F).lifetime(7));
            if(activeTicks == 0) {
                ModEventHandlerClient.firstPersonAuxParticles.add(new ParticleGluonMuzzleSmoke(player.world, 0, 0, 4.1, 0, ResourceManager.flare, 10, 50, 25).color(0.2F, 0.4F, 1F, 3F).lifetime(7));
            }
        }
        ItemGunBaseNT.setValueInt(stack, "gluon_active_ticks", Math.min(activeTicks + 1, 5));
    }
    
    public static BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> ORCHESTRA_GLUON = (stack, ctx) -> {
        EntityLivingBase entity = ctx.entity;
        HbmAnimationsSedna.AnimType type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
        ItemGunBaseNT.GunState state = ItemGunBaseNT.getState(stack, ctx.configIndex);
        
        // Clear CYCLE animation when gun is no longer in COOLDOWN state
        if(type == HbmAnimationsSedna.AnimType.CYCLE && state != ItemGunBaseNT.GunState.COOLDOWN) {
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, HbmAnimationsSedna.AnimType.EQUIP, ctx.configIndex);
            ItemGunBaseNT.setAnimTimer(stack, ctx.configIndex, 99999); // Skip to end of equip animation
        }
        
        if(entity.world.isRemote) {
            AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);
            
            if(type == HbmAnimationsSedna.AnimType.CYCLE && state == ItemGunBaseNT.GunState.COOLDOWN) {
                if(timer == 0) {
                    entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, HBMSoundHandler.gluonStart, SoundCategory.PLAYERS, 1F, 1F);
                }
                
                if(timer == 8) {
                    if(runningAudio == null || !runningAudio.isPlaying()) {
                        AudioWrapper audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.gluonLoop, SoundCategory.PLAYERS, (float) entity.posX, (float) entity.posY, (float) entity.posZ, 1F, 15F);
                        ItemGunBaseNT.loopedSounds.put(entity, audio);
                        audio.startSound();
                    }
                }
                
                if(runningAudio != null && runningAudio.isPlaying()) {
                    runningAudio.keepAlive();
                    runningAudio.updatePosition((float) entity.posX, (float) entity.posY, (float) entity.posZ);
                    
                    if(entity instanceof EntityPlayer && entity.world.getTotalWorldTime() % 2 == 0) {
                        EntityPlayer player = (EntityPlayer) entity;
                        float[] angles = getBeamDirectionOffset(player.world.getTotalWorldTime() + 1);
                        Vec3d look = Library.changeByAngle(player.getLook(1), angles[0], angles[1]);
                        RayTraceResult r = Library.rayTraceIncludeEntitiesCustomDirection(player, look, 50, 1);
                        if(r != null && r.typeOfHit == Type.ENTITY && r.entityHit instanceof EntityLivingBase) {
                            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, HBMSoundHandler.gluonHit, SoundCategory.PLAYERS, 1F, 1F);
                        }
                    }
                }
            } else {
                if(runningAudio != null && runningAudio.isPlaying()) {
                    runningAudio.stopSound();
                    entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, HBMSoundHandler.gluonEnd, SoundCategory.PLAYERS, 1F, 1F);
                }
                ItemGunBaseNT.setValueInt(stack, "gluon_active_ticks", 0);
            }
        }
    };
    
    @SuppressWarnings("incomplete-switch") 
    public static BiFunction<ItemStack, HbmAnimationsSedna.AnimType, BusAnimationSedna> LAMBDA_GLUON_ANIMS = (stack, type) -> {
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE: return new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0)); // No recoil, continuous fire
            case INSPECT: return new BusAnimationSedna()
                    .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 45, 250, IType.SIN_FULL).addPos(0, 0, 45, 350).addPos(0, 0, -15, 150, IType.SIN_FULL).addPos(0, 0, 0, 100, IType.SIN_FULL));
        }
        return null;
    };
}

