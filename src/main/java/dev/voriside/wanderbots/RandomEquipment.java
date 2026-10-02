package dev.voriside.wanderbots;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class RandomEquipment {
    private static final Material[][] ARMOR = {
            {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS},
            {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS},
            {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS},
            {Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS},
            {Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS},
            {Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS}
    };

    private static final Material[] HELD = {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE,
            Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD,
            Material.BREAD, Material.COOKED_BEEF, Material.APPLE, Material.GOLDEN_APPLE,
            Material.ENDER_PEARL, Material.SNOWBALL, Material.FIREWORK_ROCKET, Material.TORCH,
            Material.COBBLESTONE, Material.OAK_PLANKS, Material.GLASS
    };

    private static final Enchantment[] COMMON_ARMOR_ENCHANTS = {
            Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION,
            Enchantment.PROJECTILE_PROTECTION, Enchantment.UNBREAKING, Enchantment.THORNS,
            Enchantment.MENDING
    };

    private static final Enchantment[] MELEE_ENCHANTS = {
            Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.KNOCKBACK, Enchantment.FIRE_ASPECT, Enchantment.UNBREAKING,
            Enchantment.MENDING
    };

    private static final Enchantment[] BOW_ENCHANTS = {
            Enchantment.POWER, Enchantment.PUNCH, Enchantment.FLAME, Enchantment.INFINITY,
            Enchantment.UNBREAKING, Enchantment.MENDING
    };

    private RandomEquipment() {}

    static void equipRandomArmor(ServerPlayer bot) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Material[] set = ARMOR[random.nextInt(ARMOR.length)];
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < set.length; i++) {
            ItemStack bukkit = new ItemStack(set[i]);
            enchantArmor(bukkit, random);
            bot.setItemSlot(slots[i], CraftItemStack.asNMSCopy(bukkit));
        }
    }

    static void holdRandom(ServerPlayer bot, double stackChance) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Material material = HELD[random.nextInt(HELD.length)];
        ItemStack bukkit = new ItemStack(material);

        if (random.nextDouble() < 0.5) {
            if (isBow(material)) {
                addRandomEnchantments(bukkit, BOW_ENCHANTS, random);
            } else if (isMelee(material)) {
                addRandomEnchantments(bukkit, MELEE_ENCHANTS, random);
            }
        }

        if (stackChance > 0 && random.nextDouble() < stackChance && bukkit.getMaxStackSize() > 1) {
            bukkit.setAmount(random.nextInt(2, bukkit.getMaxStackSize() + 1));
        }

        bot.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(bukkit));
    }

    private static void enchantArmor(ItemStack item, ThreadLocalRandom random) {
        int count = random.nextInt(1, 4);
        addRandomEnchantments(item, COMMON_ARMOR_ENCHANTS, random, count);
    }

    private static void addRandomEnchantments(ItemStack item, Enchantment[] pool, ThreadLocalRandom random) {
        addRandomEnchantments(item, pool, random, random.nextInt(1, 3));
    }

    private static void addRandomEnchantments(ItemStack item, Enchantment[] pool, ThreadLocalRandom random, int count) {
        List<Enchantment> choices = new ArrayList<>(List.of(pool));
        for (int i = 0; i < count && !choices.isEmpty(); i++) {
            Enchantment enchantment = choices.remove(random.nextInt(choices.size()));
            int level = random.nextInt(1, Math.min(4, enchantment.getMaxLevel()) + 1);
            try {
                item.addUnsafeEnchantment(enchantment, level);
            } catch (IllegalArgumentException ignored) {
                // Some Paper registry changes can make an enchantment unavailable;
                // leave the item valid and continue.
            }
        }
    }

    private static boolean isMelee(Material material) {
        return material.name().endsWith("_SWORD") || material.name().endsWith("_AXE");
    }

    private static boolean isBow(Material material) {
        return material == Material.BOW || material == Material.CROSSBOW;
    }
}
