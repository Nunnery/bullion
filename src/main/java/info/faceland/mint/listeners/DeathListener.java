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
package info.faceland.mint.listeners;

import static org.nunnerycode.mint.MintPlugin.INT_FORMAT;

import com.tealcube.minecraft.bukkit.bullion.GoldDropEvent;
import com.tealcube.minecraft.bukkit.bullion.PlayerDeathDropEvent;
import com.tealcube.minecraft.bukkit.facecore.utilities.FireworkUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.PaletteUtil;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import info.faceland.mint.util.MintUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffectType;
import org.nunnerycode.mint.MintPlugin;

public class DeathListener implements Listener {

  private final MintPlugin plugin;
  private final List<String> noLossWorlds;
  private final double doubleDropChance;
  private final String bitBombBroadcast;

  private final Map<Player, Double> bitsDropped = new WeakHashMap<>();

  public DeathListener(MintPlugin plugin) {
    this.plugin = plugin;
    noLossWorlds = plugin.getSettings().getStringList("config.no-loss-worlds");
    doubleDropChance = plugin.getSettings().getDouble("config.double-drop-chance", 0.05);
    bitBombBroadcast = PaletteUtil.color(plugin.getSettings().getString("language.bit-bomb-message"));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onEntityDeathEvent(final EntityDeathEvent event) {
    if (!validBitDropConditions(event)) {
      return;
    }
    EntityType entityType = event.getEntityType();
    double reward = plugin.getSettings().getDouble("rewards." + entityType.name(), 0D);

    if (reward == 0D) {
      return;
    }

    String world = event.getEntity().getWorld().getName();

    double exponent = plugin.getSettings()
        .getDouble("config.money-drop-worlds." + world + ".exponential-bonus", 1D);

    String style = plugin.getSettings().getString("config.money-drop-calculation", "distance");
    if ("distance".equalsIgnoreCase(style)) {
      double multPer100Blocks = plugin.getSettings()
          .getDouble("config.money-drop-worlds." + world + ".multiplier-per-100-blocks", 0.0);
      Location worldSpawn = event.getEntity().getWorld().getSpawnLocation();
      Location entityLoc = event.getEntity().getLocation();
      double distance = worldSpawn.distance(entityLoc);
      double distMult = 1 + ((distance / 100) * multPer100Blocks);
      reward *= distMult;
    } else {
      double multPer100Levels = plugin.getSettings()
          .getDouble("config.money-drop-worlds." + world + ".multiplier-per-100-levels", 0.0);
      float level = MintUtil.getMobLevel(event.getEntity());
      reward *= 1 + ((level / 100) * multPer100Levels);
    }
    reward = Math.pow(reward, exponent);
    reward *= 0.75 + ThreadLocalRandom.current().nextDouble() / 2;
    reward *= Math.random() < doubleDropChance ? 2 : 1;

    GoldDropEvent gde = new GoldDropEvent(event.getEntity().getKiller(), event.getEntity(), reward);
    Bukkit.getPluginManager().callEvent(gde);

    if (gde.isCancelled()) {
      return;
    }

    reward = Math.max(1, gde.getAmount());

    double bombChance = plugin.getSettings().getDouble("config.bit-bomb.chance", 0.002);
    if (event.getEntity().getKiller().hasPotionEffect(PotionEffectType.LUCK)) {
      bombChance = plugin.getSettings().getDouble("config.bit-bomb.lucky-chance", 0.004);
    }

    if (ThreadLocalRandom.current().nextDouble() <= bombChance) {
      float velocity = (float) plugin.getSettings().getDouble("config.bit-bomb.velocity", 1);
      int minDrops = plugin.getSettings().getInt("config.bit-bomb.min-drops", 12);
      int maxDrops = plugin.getSettings().getInt("config.bit-bomb.max-drops", 30);

      int numberOfDrops = ThreadLocalRandom.current().nextInt(minDrops, maxDrops + 1);
      double bombTotal = 0;
      while (numberOfDrops > 0) {
        double newReward = reward * (4 + Math.random());
        bombTotal += newReward;
        Item item = MintUtil.spawnCashDrop(event.getEntity().getLocation(), newReward, velocity);
        MintUtil.applyDropProtection(item, event.getEntity().getKiller().getUniqueId(), 400);
        numberOfDrops--;
      }

      String broadcastString = bitBombBroadcast.replace("%player%", event.getEntity().getKiller().getName())
          .replace("%value%", INT_FORMAT.format(bombTotal));
      Bukkit.broadcastMessage(broadcastString);
      FireworkUtil.spawnFirework(event.getEntity().getLocation(), Type.STAR, Color.YELLOW, Color.ORANGE, false, true);
    } else {
      Item item = MintUtil.spawnCashDrop(event.getEntity().getLocation(), reward, 0);
      MintUtil.applyDropProtection(item, event.getEntity().getKiller().getUniqueId(), 400);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerDeathEvent(final PlayerDeathEvent event) {
    if (event.getEntity().getKiller() != null) {
      return;
    }
    if (event.getEntity().getLevel() <= 10) {
      return;
    }
    if (noLossWorlds.contains(event.getEntity().getWorld().getName())) {
      return;
    }

    int keptBits = 50;

    PlayerDeathDropEvent e = new PlayerDeathDropEvent(event.getEntity(), keptBits);
    Bukkit.getPluginManager().callEvent(e);

    if (e.isCancelled()) {
      return;
    }

    double dropAmount = plugin.getEconomy().getBalance(event.getEntity()) - e.getAmountProtected();
    if (dropAmount > 0) {
      plugin.getEconomy().setBalance(event.getEntity(), (int) e.getAmountProtected());
      Item item = MintUtil.spawnCashDrop(event.getEntity().getLocation(), dropAmount, 0);
      MintUtil.applyDropProtection(item, event.getEntity().getUniqueId(), 2400);
      bitsDropped.put(event.getPlayer(), dropAmount);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onRespawn(final PlayerRespawnEvent event) {
    if (!bitsDropped.containsKey(event.getPlayer())) {
      return;
    }
    double value = bitsDropped.remove(event.getPlayer());
    if (value > 0) {
      PaletteUtil.sendMessage(event.getPlayer(), "|crimson| Bits Dropped: |yellow|" + INT_FORMAT.format(value) + "◎");
    }
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      PaletteUtil.sendMessage(event.getPlayer(),
          "|yellow||i|You dropped some Bits! You can pick them back up again, if you get there quickly enough! :O");
    }, 52L);
  }

  private boolean validBitDropConditions(EntityDeathEvent event) {
    if (event instanceof PlayerDeathEvent || event.getEntity().getKiller() == null) {
      return false;
    }
    if (StringUtils.isBlank(event.getEntity().getCustomName())) {
      return false;
    }
    if (event.getEntity().getCustomName().startsWith(ChatColor.WHITE + "Spawned")) {
      return false;
    }
    String dropWorld = event.getEntity().getWorld().getName();
    if (!plugin.getSettings()
        .getBoolean("config.money-drop-worlds." + dropWorld + ".enabled", false)) {
      return false;
    }
    if (ThreadLocalRandom.current().nextDouble() > plugin.getSettings()
        .getDouble("config.money-drop-worlds." + dropWorld + ".drop-chance", 1.0)) {
      return false;
    }
    return true;
  }
}
