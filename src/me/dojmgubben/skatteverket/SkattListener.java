package me.dojmgubben.skatteverket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SkattListener implements Listener {
	public static Skatteverket plugin;

	public SkattListener(Skatteverket instance) {
		plugin = instance;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Skatteverket.openConnection();
		try {
			String lastTaxed = plugin.getConfig().getString(
					"player." + e.getPlayer().getName());

			if (Skatteverket.playerDataContainsPlayer(e.getPlayer())) {
				PreparedStatement sql = Skatteverket.connection
						.prepareStatement("SELECT "
								+ plugin.getConfig().getString("MySQL.date")
								+ " FROM '"
								+ plugin.getConfig().getString("MySQL.table")
								+ "' WHERE player=?;");
				sql.setString(1, e.getPlayer().getName());

				ResultSet result = sql.executeQuery();
				result.next();

				lastTaxed = result.getString(plugin.getConfig().getString(
						"players." + e.getPlayer().getName()));

				PreparedStatement taxedUpdate = Skatteverket.connection
						.prepareStatement("UPDATE '"
								+ plugin.getConfig().getString("MySQL.table")
								+ "' SET "
								+ plugin.getConfig().getString("MySQL.date")
								+ "=? WHERE player=?;");
				taxedUpdate.setString(
						1,
						plugin.getConfig().getString(
								"players." + e.getPlayer().getName()));
				taxedUpdate.setString(2, e.getPlayer().getName());
				taxedUpdate.executeUpdate();

				taxedUpdate.close();
				sql.close();
				result.close();
			} else {
				PreparedStatement newPlayer = Skatteverket.connection
						.prepareStatement("INSERT INTO '"
								+ plugin.getConfig().getString("MySQL.table")
								+ "' values(?,"
								+ plugin.getConfig().getString(
										"players." + e.getPlayer() + ");"));
				newPlayer.setString(1, e.getPlayer().getName());
				newPlayer.execute();
				newPlayer.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			Skatteverket.closeConnection();
		}
		Player player = e.getPlayer();
		if (plugin.getConfig().getBoolean("general.AutoChargeDaily"))
			plugin.CollectTaxes(player);
	}
}