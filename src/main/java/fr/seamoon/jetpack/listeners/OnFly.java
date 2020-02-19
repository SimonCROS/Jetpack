package fr.seamoon.jetpack.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.seamoon.jetpack.JetpackItem;
import fr.seamoon.jetpack.api.events.PlayerFlyWithJetpackEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class OnFly implements Listener {

//	private JetpackMain main = JetpackMain.getInstance();

	@EventHandler
	public void onFly(PlayerFlyWithJetpackEvent e) {
		Player p = e.getPlayer();
		JetpackItem item = e.getItem();
		float leftLevel = item.getLeftGasLevel();
		float rightLevel = item.getRightGasLevel();
		int bars = 50;

		int leftRedBars = Math.round(bars * leftLevel / 100);
		int leftEmptyBars = bars - leftRedBars;

		int rightRedBars = Math.round(bars * rightLevel / 100);
		int rightEmptyBars = bars - rightRedBars;

		String left = Math.round(leftLevel) + "§4 " + new String(new char[leftRedBars]).replace("\0", "|") + "§f"
				+ new String(new char[leftEmptyBars]).replace("\0", "|");
		String right = Math.round(rightLevel) + "§4 " + new String(new char[rightRedBars]).replace("\0", "|") + "§f"
				+ new String(new char[rightEmptyBars]).replace("\0", "|");
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(left + "    §r" + right));

		if (leftLevel <= 0 && rightLevel <= 0) {
			e.setCancelled(true);
		} else if (leftLevel <= 0) {
			item.setRightGasLevel(rightLevel - 0.2f);
		} else if (rightLevel <= 0) {
			item.setLeftGasLevel(leftLevel - 0.2f);
		} else {
			item.setRightGasLevel(rightLevel - 0.1f);
			item.setLeftGasLevel(leftLevel - 0.1f);
		}
	}
}
