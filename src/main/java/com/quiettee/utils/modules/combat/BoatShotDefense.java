package com.quiettee.utils.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

final class BoatShotDefense {
    static int drawTicks(LivingEntity target,ItemStack bow,double burst) {
        var levels=new Object2IntOpenHashMap<Holder<Enchantment>>();
        int protection=0;
        for(EquipmentSlot slot:EquipmentSlotGroup.ARMOR) {
            Utils.getEnchantments(target.getItemBySlot(slot),levels);
            protection+=Utils.getEnchantmentLevel(levels,Enchantments.PROTECTION)
                +2*Utils.getEnchantmentLevel(levels,Enchantments.PROJECTILE_PROTECTION);
        }
        Utils.getEnchantments(bow,levels);
        int power=Utils.getEnchantmentLevel(levels,Enchantments.POWER);
        var resistance=target.getEffect(MobEffects.RESISTANCE);
        return BoatShotDamage.drawTicks(burst,power,new BoatShotDamage.Defense(
            target.getHealth()+target.getAbsorptionAmount(),target.getArmorValue(),target.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
            protection,resistance==null?0:resistance.getAmplifier()+1));
    }
}
