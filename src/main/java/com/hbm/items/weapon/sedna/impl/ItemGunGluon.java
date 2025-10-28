package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.render.NTMRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public class ItemGunGluon extends ItemGunBaseNT {

    public ItemGunGluon(WeaponQuality quality, String s, GunConfig... cfg) {
        super(quality, s, cfg);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUD(RenderGameOverlayEvent.Pre event, RenderGameOverlayEvent.ElementType type, EntityPlayer player, ItemStack stack, EnumHand hand) {
        super.renderHUD(event, type, player, stack, hand);
        
        // Render custom gluon crosshair
        if(type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            ScaledResolution res = event.getResolution();
            float x = res.getScaledWidth() / 2;
            float y = res.getScaledHeight() / 2;
            
            Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.gluontau_hud);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager.color(0.9F, 0.9F, 0F, 1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            NTMRenderHelper.drawGuiRect(x - 2F, y - 2F, 0, 0, 4, 4, 1, 1);
            NTMRenderHelper.resetColor();
            GlStateManager.disableBlend();
        }
    }
}

