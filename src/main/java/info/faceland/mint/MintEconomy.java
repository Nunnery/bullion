/**
 * The MIT License Copyright (c) 2015 Teal Cube Games
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package info.faceland.mint;

import com.tealcube.minecraft.bukkit.bullion.MoneyChangeEvent;
import io.pixeloutlaw.minecraft.spigot.garbage.StringExtensionsKt;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.nunnerycode.mint.MintPlugin;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public class MintEconomy implements Economy {

  private static final DecimalFormat DF = new DecimalFormat("###,###,###");
  private final String currencyPlural;
  private final String currencySingular;
  private final MintPlugin plugin;

  public MintEconomy(MintPlugin plugin) {
    this.plugin = plugin;
    currencyPlural = StringExtensionsKt.chatColorize(
        plugin.getSettings().getString("config.currency-plural", ChatColor.YELLOW + "◎"));
    currencySingular = StringExtensionsKt.chatColorize(
        plugin.getSettings().getString("config.currency-singular", ChatColor.YELLOW + "◎"));
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return "Mint";
  }

  @Override
  public boolean hasBankSupport() {
    return true;
  }

  @Override
  public int fractionalDigits() {
    return 0;
  }

  @Override
  public String format(double v) {
    if (Math.floor(v) == 1.00D) {
      return String.format("%s%s", DF.format(v), currencyNameSingular());
    }
    return String.format("%s%s", DF.format(v), currencyNamePlural());
  }

  @Override
  public String currencyNamePlural() {
    return currencyPlural;
  }

  @Override
  public String currencyNameSingular() {
    return currencySingular;
  }

  @Override
  public boolean hasAccount(String s) {
    // logger.debug("hasAccount({})", s);
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    return plugin.getManager().hasPlayerAccount(uuid) ||
        createPlayerAccount(s);
  }

  @Override
  public boolean hasAccount(OfflinePlayer player) {
    return hasAccount(player.getUniqueId().toString());
  }

  @Override
  public boolean hasAccount(String s, String s2) {
    return hasAccount(s);
  }

  @Override
  public boolean hasAccount(OfflinePlayer player, String worldName) {
    return hasAccount(player);
  }

  @Override
  public double getBalance(String s) {
    if (!hasAccount(s)) {
      return 0;
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    return (double) Math.round(plugin.getManager().getPlayerBalance(uuid) * 100) / 100;
  }

  @Override
  public double getBalance(OfflinePlayer player) {
    return getBalance(player.getUniqueId().toString());
  }

  @Override
  public double getBalance(String s, String s2) {
    return getBalance(s);
  }

  @Override
  public double getBalance(OfflinePlayer player, String world) {
    return getBalance(player);
  }

  @Override
  public boolean has(String s, double v) {
    // logger.debug("has({}, {})", s, v);
    return hasAccount(s) && getBalance(s) >= v;
  }

  @Override
  public boolean has(OfflinePlayer player, double amount) {
    return has(player.getUniqueId().toString(), amount);
  }

  @Override
  public boolean has(String s, String s2, double v) {
    return has(s, v);
  }

  @Override
  public boolean has(OfflinePlayer player, String worldName, double amount) {
    return has(player, amount);
  }

  @Override
  public EconomyResponse withdrawPlayer(String s, double v) {
    // logger.debug("withdrawPlayer({}, {})", s, v);
    if (!hasAccount(s)) {
      createPlayerAccount(s);
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    double balance = plugin.getManager().getPlayerBalance(uuid);
    if (!has(s, v)) {
      return new EconomyResponse(v, balance, EconomyResponse.ResponseType.FAILURE, null);
    }
    double newBalance = balance - Math.abs(v);
    plugin.getManager().setPlayerBalance(uuid, newBalance);
    EconomyResponse response =
        new EconomyResponse(v, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
    Bukkit.getPluginManager().callEvent(new MoneyChangeEvent(uuid, balance, newBalance));
    return response;
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
    return withdrawPlayer(player.getUniqueId().toString(), amount);
  }

  @Override
  public EconomyResponse withdrawPlayer(String s, String s2, double v) {
    return withdrawPlayer(s, v);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
    return withdrawPlayer(player, amount);
  }

  @Override
  public EconomyResponse depositPlayer(String s, double v) {
    // logger.debug("depositPlayer({}, {})", s, v);
    if (!hasAccount(s)) {
      createPlayerAccount(s);
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    double balance = plugin.getManager().getPlayerBalance(uuid);
    double newBalance = balance + Math.abs(v);
    plugin.getManager().setPlayerBalance(uuid, newBalance);
    EconomyResponse response = new EconomyResponse(v, balance + Math.abs(v),
        EconomyResponse.ResponseType.SUCCESS, null);
    Bukkit.getPluginManager().callEvent(new MoneyChangeEvent(uuid, balance, newBalance));
    return response;
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
    return depositPlayer(player.getUniqueId().toString(), amount);
  }

  @Override
  public EconomyResponse depositPlayer(String s, String s2, double v) {
    return depositPlayer(s, v);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
    return depositPlayer(player, amount);
  }

  @Override
  public EconomyResponse createBank(String s, String s2) {
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    plugin.getManager().setBankBalance(uuid, 0D);
    return new EconomyResponse(0D, plugin.getManager().getBankBalance(uuid),
        EconomyResponse.ResponseType.SUCCESS, null);
  }

  @Override
  public EconomyResponse createBank(String name, OfflinePlayer player) {
    return createBank(name, player.getUniqueId().toString());
  }

  @Override
  public EconomyResponse deleteBank(String s) {
    plugin.getManager().removeBankAccount(UUID.fromString(s));
    return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.SUCCESS, null);
  }

  @Override
  public EconomyResponse bankBalance(String s) {
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    if (plugin.getManager().hasBankAccount(uuid)) {
      double balance = plugin.getManager().getBankBalance(uuid);
      return new EconomyResponse(balance, balance, EconomyResponse.ResponseType.SUCCESS, null);
    }
    return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.FAILURE, null);
  }

  @Override
  public EconomyResponse bankHas(String s, double v) {
    EconomyResponse response = bankBalance(s);
    if (response.transactionSuccess()) {
      if (response.balance >= v) {
        return new EconomyResponse(0D, response.balance, EconomyResponse.ResponseType.SUCCESS,
            null);
      }
      return new EconomyResponse(0D, response.balance, EconomyResponse.ResponseType.FAILURE, null);
    }
    return new EconomyResponse(0D, response.balance, EconomyResponse.ResponseType.FAILURE, null);
  }

  @Override
  public EconomyResponse bankWithdraw(String s, double v) {
    EconomyResponse response = bankBalance(s);
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    double balance = plugin.getManager().getBankBalance(uuid);
    if (response.transactionSuccess()) {
      plugin.getManager().setBankBalance(uuid, balance - Math.abs(v));
      return new EconomyResponse(v, balance - Math.abs(v), EconomyResponse.ResponseType.SUCCESS,
          null);
    }
    return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.FAILURE, null);
  }

  @Override
  public EconomyResponse bankDeposit(String s, double v) {
    EconomyResponse response = bankBalance(s);
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    double balance = plugin.getManager().getBankBalance(uuid);
    if (response.transactionSuccess()) {
      plugin.getManager().setBankBalance(uuid, balance + Math.abs(v));
      return new EconomyResponse(v, balance + Math.abs(v), EconomyResponse.ResponseType.SUCCESS,
          null);
    }
    return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.FAILURE, null);
  }

  @Override
  public EconomyResponse isBankOwner(String s, String s2) {
    if (s.equals(s2)) {
      return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.SUCCESS, null);
    }
    return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.FAILURE, null);
  }

  @Override
  public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
    return isBankOwner(name, player.getUniqueId().toString());
  }

  @Override
  public EconomyResponse isBankMember(String s, String s2) {
    return isBankOwner(s, s2);
  }

  @Override
  public EconomyResponse isBankMember(String name, OfflinePlayer player) {
    return isBankMember(name, player.getUniqueId().toString());
  }

  @Override
  public List<String> getBanks() {
    return plugin.getManager().banksAsStrings();
  }

  @Override
  public boolean createPlayerAccount(String s) {
    // logger.debug("createPlayerAccount({})", s);
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    plugin.getManager().setPlayerBalance(uuid, 0D);
    Bukkit.getPluginManager().callEvent(new MoneyChangeEvent(uuid, 0, 0));
    return true;
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player) {
    return createPlayerAccount(player.getUniqueId().toString());
  }

  @Override
  public boolean createPlayerAccount(String s, String s2) {
    return createPlayerAccount(s);
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
    return createPlayerAccount(player);
  }

  public EconomyResponse setBalance(OfflinePlayer player, int v) {
    return setBalance(player.getUniqueId().toString(), v);
  }

  public EconomyResponse setBalance(String s, double v) {
    // logger.debug("setBalance({}, {})", s, v);
    if (!hasAccount(s)) {
      createPlayerAccount(s);
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      uuid = Bukkit.getOfflinePlayer(s).getUniqueId();
    }
    double d = plugin.getManager().getPlayerBalance(uuid);
    plugin.getManager().setPlayerBalance(uuid, v);
    Bukkit.getPluginManager().callEvent(new MoneyChangeEvent(uuid, d, v));
    return new EconomyResponse(d - v, v, EconomyResponse.ResponseType.SUCCESS, null);
  }

}
