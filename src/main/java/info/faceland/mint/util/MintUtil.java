/**
 * The MIT License Copyright (c) 2015 Teal Cube Games
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package info.faceland.mint.util;

import com.tealcube.minecraft.bukkit.facecore.FacecorePlugin;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.math.NumberUtils;
import com.tealcube.minecraft.bukkit.shade.google.common.base.CharMatcher;
import info.faceland.mint.pojo.RecentPickupEarnings;
import info.faceland.mint.tasks.PickupTask;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.nunnerycode.mint.MintPlugin;

public class MintUtil {

  public static Map<UUID, Double> protectedCashCache = new HashMap<>();
  public static Map<Player, RecentPickupEarnings> recentEarnings = new WeakHashMap<>();
  public static String CASH_STRING = ChatColor.GOLD + "REWARD!";
  private static final Random random = new Random();

  public static void setProtectedCash(Player player, double amount) {
    protectedCashCache.put(player.getUniqueId(), amount);
  }

  public static double getProtectedCash(Player player) {
    return protectedCashCache.getOrDefault(player.getUniqueId(), 0D);
  }

  public static boolean doCashPickup(Player player, Item item) {
    ItemStack itemStack = item.getItemStack();

    if (itemStack.getType() != Material.GOLD_NUGGET) {
      return false;
    }
    if (!MintUtil.CASH_STRING.equals(ItemStackExtensionsKt.getDisplayName(itemStack))) {
      return false;
    }

    item.remove();
    player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0F, 1.3F);

    String stripped = ChatColor.stripColor(itemStack.getLore().get(0));
    String replaced = CharMatcher.forPredicate(Character::isLetter).removeFrom(stripped).trim();
    int stacksize = itemStack.getAmount();

    double amount = stacksize * NumberUtils.toDouble(replaced);
    MintPlugin.getInstance().getEconomy().depositPlayer(player, amount);

    if (!recentEarnings.containsKey(player)) {
      recentEarnings.put(player, new RecentPickupEarnings(amount));
    } else {
      RecentPickupEarnings earnings = recentEarnings.get(player);
      if (System.currentTimeMillis() > earnings.getTimestamp()) {
        earnings.setAmount(amount);
      } else {
        earnings.setAmount(earnings.getAmount() + amount);
      }
      earnings.setTimestamp(System.currentTimeMillis() + 1250);
    }

    return true;
  }

  public static Item spawnCashDrop(Location location, double amount) {
    return spawnCashDrop(location, amount, 0);
  }

  public static Item spawnCashDrop(Location location, double amount, float velocity) {
    velocity = Math.max(0.2f, velocity);
    ItemStack item = new ItemStack(Material.GOLD_NUGGET);
    ItemStackExtensionsKt.setDisplayName(item, CASH_STRING);
    item.setLore(Collections.singletonList(Double.toString(amount)));
    Item droppedItem = location.getWorld().spawn(location, Item.class, d -> d.setItemStack(item));
    droppedItem.setVelocity(new Vector(
        Math.random() * velocity * (Math.random() > 0.5 ? 1 : -1),
        0.1 + Math.random() * velocity,
        Math.random() * velocity * (Math.random() > 0.5 ? 1 : -1))
    );
    applyNameplate(droppedItem, amount);
    droppedItem.getPersistentDataContainer().set(MintPlugin.moneyKey, PersistentDataType.SHORT, (short) 1);
    new PickupTask(droppedItem);
    return droppedItem;
  }

  private static void applyNameplate(Item item, double amount) {
    ItemStack nuggetStack = item.getItemStack();
    ItemStackExtensionsKt.setDisplayName(nuggetStack, MintUtil.CASH_STRING);

    int amountId = (int) Math.floor(amount);
    if (amountId < 2) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 1000000 + random.nextInt(999999));
    } else if (amountId == 2) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 2000000 + random.nextInt(999999));
    } else if (amountId == 3) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 3000000 + random.nextInt(999999));
    } else if (amountId < 12) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 4000000 + random.nextInt(999999));
    } else if (amountId < 80) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 5000000 + random.nextInt(999999));
    } else if (amountId < 250) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 6000000 + random.nextInt(999999));
    } else if (amountId < 999) {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 7000000 + random.nextInt(999999));
    } else {
      ItemStackExtensionsKt.setCustomModelData(nuggetStack, 8000000 + random.nextInt(999999));
    }

    item.setItemStack(nuggetStack);
    item.setCustomName(FaceColor.YELLOW.s() + amountId + ChatColor.YELLOW + "◎");
    item.setCustomNameVisible(true);
  }

  public static void applyDropProtection(Item drop, UUID owner, long duration) {
    drop.setOwner(owner);
    Bukkit.getScheduler().runTaskLater(MintPlugin.getInstance(), () -> clearDropProtection(drop), duration);
  }

  public static void clearDropProtection(Item drop) {
    if (drop != null) {
      drop.setOwner(null);
    }
  }

  public static boolean isCashDrop(ItemStack itemStack) {
    return itemStack.getType() == Material.GOLD_NUGGET && itemStack.getItemMeta() != null
        && CASH_STRING.equals(itemStack.getItemMeta().getDisplayName())
        && itemStack.getItemMeta().getLore() != null && !itemStack.getItemMeta().getLore()
        .isEmpty();
  }

  public static int getMobLevel(LivingEntity livingEntity) {
    int level;
    if (livingEntity instanceof Player) {
      level = ((Player) livingEntity).getLevel();
    } else if (livingEntity.hasMetadata("LVL")) {
      level = livingEntity.getMetadata("LVL").get(0).asInt();
    } else if (StringUtils.isBlank(livingEntity.getCustomName())) {
      level = 0;
    } else {
      String lev = CharMatcher.digit().or(CharMatcher.is('-')).negate()
          .collapseFrom(ChatColor.stripColor(livingEntity.getCustomName()), ' ').trim();
      level = NumberUtils.toInt(lev.split(" ")[0], 0);
    }
    return level;
  }
}
