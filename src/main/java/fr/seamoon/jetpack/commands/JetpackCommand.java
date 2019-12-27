package fr.seamoon.jetpack.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.seamoon.jetpack.Jetpack;

public class JetpackCommand implements CommandExecutor {
	
	private Jetpack jetpack = Jetpack.getInstance();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("jetpack")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				jetpack.spawnJetpack(p);
			}
		}
		return false;
	}
}
