package fr.seamoon.jetpack.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JetpackCommand implements CommandExecutor {
	
//	private JetpackMain main = JetpackMain.getInstance();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("jetpack")) {
			if (sender instanceof Player) {
//				Player p = (Player) sender;
//				BossBar bar = Bukkit.createBossBar("Jetpack", BarColor.RED, BarStyle.SOLID);
//				bar.addPlayer(p);
				
			}
		}
		return false;
	}
}
