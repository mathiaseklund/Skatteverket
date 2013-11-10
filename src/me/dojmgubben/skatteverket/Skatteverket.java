package me.dojmgubben.skatteverket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Skatteverket extends JavaPlugin {
	public static Economy ep;
	protected static Logger log;
	public static Skatteverket instance;
	SkattCommand sc;
	private final SkattListener sL = new SkattListener(this);
	File configFile;
	FileConfiguration config;
	public static Connection connection;

	public void onEnable() {
		setupEconomy();
		instance = this;
		log = getLogger();

		this.sc = new SkattCommand(this);
		this.configFile = new File(getDataFolder(), "config.yml");

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.sL, this);
		try {
			firstRun();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.config = new YamlConfiguration();
		loadYamls();
	}

	public void onDisable() {
		try {
			if (connection != null && !connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public synchronized static void closeConnection() {
		try {
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized static void openConnection() {
		try {
			connection = DriverManager.getConnection("jdbc:mysql://"
					+ instance.getConfig().getString("MySQL.host") + ":"
					+ instance.getConfig().getString("MySQL.port") + "/"
					+ instance.getConfig().getString("MySQL.database"),
					instance.getConfig().getString("MySQL.user"), instance
							.getConfig().getString("MySQL.pass"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized static boolean playerDataContainsPlayer(Player player) {
		try {
			PreparedStatement sql = connection
					.prepareStatement("SELECT * FROM '"
							+ instance.getConfig().getString("MySQL.table")
							+ "' WHERE player=?;");
			sql.setString(1, player.getName());
			ResultSet resultSet = sql.executeQuery();
			boolean containsPlayer = resultSet.next();

			sql.close();
			resultSet.close();

			return containsPlayer;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("rawtypes")
	private boolean setupEconomy() {
		RegisteredServiceProvider economyProvider = getServer()
				.getServicesManager().getRegistration(Economy.class);
		ep = (Economy) economyProvider.getProvider();
		return ep != null;
	}

	public void firstRun() throws Exception {
		if (!this.configFile.exists()) {
			this.configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), this.configFile);
			log.info("Config not found, Generating.");
		}
	}

	private void loadYamls() {
		try {
			this.config.load(this.configFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveYamls() {
		try {
			this.config.save(this.configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Skatteverket getInstance() {
		return instance;
	}

	public String getCurrentDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public void CollectTaxes(Player player) {
		String LastPaid = getConfig().getString("players." + player.getName());
		String date = getCurrentDate();

		if (LastPaid == date) {
			player.sendMessage("LAST PAID EQUALS DATE");
			return;
		}

		if (player.hasPermission("skatteverket.vip")) {
			return;
		}

		double balance = ep.getBalance(player.getName());
		int low = getConfig().getInt("krav.low");
		int middle = getConfig().getInt("krav.middle");
		int high = getConfig().getInt("krav.high");
		double lowtax = balance * getConfig().getDouble("skatteklass.low");
		double middletax = balance
				* getConfig().getDouble("skatteklass.middle");
		double hightax = balance * getConfig().getDouble("skatteklass.high");
		double maxlowtax = getConfig().getDouble("maxskatt.low");
		double maxmiddletax = getConfig().getDouble("maxskatt.middle");
		double maxhightax = getConfig().getDouble("maxskatt.high");
		String serveraccount = getConfig().getString("general.account");
		String SkattNotis = ChatColor
				.translateAlternateColorCodes(
						'&',
						getConfig()
								.getString(
										"general.notis",
										"&6[Skatteverket] &7Du har blivit beskattad för ett nytt dygn på servern! /skatteinfo för mer information"));
		if (getConfig().get("players." + player.getName()) == null) {
			player.sendMessage(SkattNotis);

			if ((balance >= low) && (balance < middle)) {
				String rounded = ep.format(lowtax);
				if (lowtax > maxlowtax) {
					ep.withdrawPlayer(player.getName(), maxlowtax);
					ep.depositPlayer(serveraccount, maxlowtax);
					player.sendMessage("§3Du betalade §6" + maxlowtax
							+ " §3i skatt.");
				} else {
					ep.withdrawPlayer(player.getName(), lowtax);
					ep.depositPlayer(serveraccount, lowtax);
					player.sendMessage("§3Du betalade §6" + rounded
							+ " §3i skatt.");
				}
			}

			if ((balance >= middle) && (balance < high)) {
				String rounded = ep.format(middletax);
				if (middletax > maxmiddletax) {
					ep.withdrawPlayer(player.getName(), maxmiddletax);
					ep.depositPlayer(serveraccount, maxmiddletax);
					player.sendMessage("§3Du betalade §6" + maxmiddletax
							+ " §3i skatt.");
				} else {
					ep.withdrawPlayer(player.getName(), middletax);
					ep.depositPlayer(serveraccount, middletax);
					player.sendMessage("§3Du betalade §6" + rounded
							+ " §3i skatt.");
				}
			}

			if (balance > high) {
				if (hightax > maxhightax) {
					ep.withdrawPlayer(player.getName(), maxhightax);
					ep.depositPlayer(serveraccount, maxhightax);
					player.sendMessage("§3Du betalade §6" + maxhightax
							+ " §3i skatt.");
				} else {
					String rounded = ep.format(hightax);
					ep.withdrawPlayer(player.getName(), hightax);
					ep.depositPlayer(serveraccount, hightax);
					player.sendMessage("§3Du betalade §6" + rounded
							+ " §3i skatt.");
				}
			}

			getConfig().set("players." + player.getName(), date);
			saveConfig();

			return;
		}
		if (!getConfig().getString("players." + player.getName()).equals(date)) {
			player.sendMessage(SkattNotis);

			if ((balance >= low) && (balance < middle)) {
				String rounded = ep.format(lowtax);
				if (lowtax > maxlowtax) {
					ep.withdrawPlayer(player.getName(), maxlowtax);
					ep.depositPlayer(serveraccount, maxlowtax);
					player.sendMessage("§3Du betalade §6" + maxlowtax
							+ " §3i skatt.");
				} else {
					ep.withdrawPlayer(player.getName(), lowtax);
					ep.depositPlayer(serveraccount, lowtax);
					player.sendMessage("§3Du betalade §6" + rounded
							+ " §3i skatt.");
				}
			}

			if ((balance >= middle) && (balance < high)) {
				String rounded = ep.format(middletax);
				if (middletax > maxmiddletax) {
					ep.withdrawPlayer(player.getName(), maxmiddletax);
					ep.depositPlayer(serveraccount, maxmiddletax);
					player.sendMessage("§3Du betalade §6" + maxmiddletax
							+ " §3i skatt.");
				} else {
					ep.withdrawPlayer(player.getName(), middletax);
					ep.depositPlayer(serveraccount, middletax);
					player.sendMessage("§3Du betalade §6" + rounded
							+ " §3i skatt.");
				}
			}

			if (balance > high) {
				if (hightax > maxhightax) {
					ep.withdrawPlayer(player.getName(), maxhightax);
					ep.depositPlayer(serveraccount, maxhightax);
					player.sendMessage("§3Du betalade §6" + maxhightax
							+ " §3i skatt.");
				} else {
					String rounded = ep.format(hightax);
					ep.withdrawPlayer(player.getName(), hightax);
					ep.depositPlayer(serveraccount, hightax);
					player.sendMessage("§3Du betalade §6" + rounded
							+ " §3i skatt.");
				}
			}

			getConfig().set("players." + player.getName(), date);
			saveConfig();

			return;
		}
	}
}