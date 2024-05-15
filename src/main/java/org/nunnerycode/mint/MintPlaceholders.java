package org.nunnerycode.mint;

import static org.nunnerycode.mint.MintPlugin.INT_FORMAT;

import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import info.faceland.mint.util.MintUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MintPlaceholders extends PlaceholderExpansion {

  @Override
  public @NotNull String getAuthor() {
    return "Faceguy";
  }

  @Override
  public @NotNull String getIdentifier() {
    return "mint";
  }

  @Override
  public @NotNull String getVersion() {
    return "1.0.0";
  }

  @Override
  public boolean persist(){
    return true;
  }

  @Override
  public String onPlaceholderRequest(Player p, @NotNull String placeholder) {
    if (p == null || StringUtils.isBlank(placeholder)) {
      return "";
    }
    if (placeholder.startsWith("max_protected_money")) {
      return INT_FORMAT.format(MintUtil.getProtectedCash(p));
    }
    if (placeholder.startsWith("bank_balance")) {
      return INT_FORMAT.format(MintPlugin.getInstance().getManager().getBankBalance(p.getUniqueId()));
    }
    return null;
  }
}
