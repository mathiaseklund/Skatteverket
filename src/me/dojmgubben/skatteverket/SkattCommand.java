package me.dojmgubben.skatteverket;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

public class SkattCommand {
	Skatteverket plugin;

	public SkattCommand(Skatteverket instance) {
		this.plugin = instance;
		init();
	}

	private void init() {
		PluginCommand skatteinfo = this.plugin.getCommand("skatteinfo");

		CommandExecutor exe = new CommandExecutor() {
			public boolean onCommand(CommandSender s, Command c, String label,
					String[] args) {
				int low;
				int middle;
				if (args.length < 1) {
					double balance = Skatteverket.ep.getBalance(s.getName());
					low = SkattCommand.this.plugin.getConfig().getInt(
							"krav.low");
					middle = SkattCommand.this.plugin.getConfig().getInt(
							"krav.middle");
					int high = SkattCommand.this.plugin.getConfig().getInt(
							"krav.high");
					double lowpercent = SkattCommand.this.plugin.getConfig()
							.getDouble("skatteklass.low") * 100.0D;
					double middlepercent = SkattCommand.this.plugin.getConfig()
							.getDouble("skatteklass.middle") * 100.0D;
					double highpercent = SkattCommand.this.plugin.getConfig()
							.getDouble("skatteklass.high") * 100.0D;
					double lowtax = balance
							* SkattCommand.this.plugin.getConfig().getDouble(
									"skatteklass.low");
					double middletax = balance
							* SkattCommand.this.plugin.getConfig().getDouble(
									"skatteklass.middle");
					double hightax = balance
							* SkattCommand.this.plugin.getConfig().getDouble(
									"skatteklass.high");
					double maxlowtax = SkattCommand.this.plugin.getConfig()
							.getDouble("maxskatt.low");
					double maxmiddletax = SkattCommand.this.plugin.getConfig()
							.getDouble("maxskatt.middle");
					double maxhightax = SkattCommand.this.plugin.getConfig()
							.getDouble("maxskatt.high");

					if ((balance >= low) && (balance < middle)) {
						String rounded = Skatteverket.ep.format(lowtax);
						s.sendMessage("§3Skatteklass: §aLow");
						s.sendMessage("§3Skatteklass: §a" + lowpercent + "%");
						if (lowtax > maxlowtax)
							s.sendMessage("§3Skatt som ska betalas: §a" + maxlowtax);
						else {
							s.sendMessage("§3Skatt som ska betalas: §a" + rounded);
						}
					}
					if ((balance >= middle) && (balance < high)) {
						String rounded = Skatteverket.ep.format(middletax);
						s.sendMessage("§3Skatteklass: §aMiddle");
						s.sendMessage("§3Skatt procent: §a" + middlepercent
								+ "%");
						if (middletax > maxmiddletax)
							s.sendMessage("§3Skatt som ska betalas: §a" + maxmiddletax);
						else {
							s.sendMessage("§3Skatt som ska betalas: §a" + rounded);
						}
					}

					if (balance >= high) {
						s.sendMessage("§3Skatteklass: §aHigh");
						String rounded = Skatteverket.ep.format(hightax);
						s.sendMessage("§3Skatt procent: §a" + highpercent + "%");
						if (hightax > maxhightax)
							s.sendMessage("§3Skatt som ska betalas: §a" + maxhightax);
						else {
							s.sendMessage("§3Skatt som ska betalas: §a" + rounded);
						}
					}
					s.sendMessage("§3Senast betalda skatt: §a"
							+ SkattCommand.this.plugin.getConfig().getString(
									new StringBuilder("players.").append(
											s.getName()).toString()));
					s.sendMessage("§3Saldo: §a" + balance);
					if (s.hasPermission("skatteverket.vip"))
						s.sendMessage("§3Status: §cUndantagen");
					else if (!s.hasPermission("skatteverket.vip")) {
						s.sendMessage("§3Status: §aTillämplig");
					}
					return true;
				}
				return true;
			}
		};
		skatteinfo.setExecutor(exe);
	}
}
