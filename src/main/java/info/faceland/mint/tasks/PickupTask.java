package info.faceland.mint.tasks;

import info.faceland.mint.util.MintUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nunnerycode.mint.MintPlugin;

public class PickupTask extends BukkitRunnable {

  private static final DistanceComparator distanceComparator = new DistanceComparator();

  private final Item sourceItem;

  public PickupTask(Item sourceItem) {
    this.sourceItem = sourceItem;
    runTaskTimer(MintPlugin.getInstance(), 10 + (int) (20 * Math.random()), 6L);
  }

  @Override
  public void run() {
    if (sourceItem == null || !sourceItem.isValid()) {
      cancel();
      return;
    }

    List<Player> players = new ArrayList<>(sourceItem.getWorld()
        .getNearbyPlayers(sourceItem.getLocation(), 1.5));

    if (players.isEmpty()) {
      return;
    }

    if (players.size() > 1) {
      players = players.stream().filter(e -> !e.isDead() && e.getHealth() > 0 &&
          e.getWorld() == sourceItem.getWorld()).collect(Collectors.toList());
    }

    if (sourceItem.getOwner() == null) {
      MintUtil.doCashPickup(players.get(0), sourceItem);
      cancel();
      return;
    }

    for (Player p : players) {
      if (!p.getUniqueId().equals(sourceItem.getOwner())) {
        continue;
      }
      if (MintUtil.doCashPickup(p, sourceItem)) {
        cancel();
        return;
      }
    }
  }

  private static class DistanceComparator implements Comparator<LivingEntity> {

    private Location loc;

    public int compare(LivingEntity le1, LivingEntity le2) {
      return Double.compare(le1.getLocation().distanceSquared(loc), le2.getLocation().distanceSquared(loc));
    }

    public void setLoc(Location loc) {
      this.loc = loc;
    }
  }
}
