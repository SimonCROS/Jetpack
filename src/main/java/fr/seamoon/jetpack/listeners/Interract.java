package fr.seamoon.jetpack.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import fr.seamoon.jetpack.JetpackItem;
import fr.seamoon.jetpack.JetpackMain;

public class Interract implements Listener {

	private JetpackMain main = JetpackMain.getInstance();

	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		JetpackItem item;
		if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
				&& e.getHand().equals(EquipmentSlot.HAND) && (item = JetpackItem.fromItemStack(e.getItem())) != null) {
			if (item.getAmount() > 1) {
				p.sendMessage(ChatColor.RED + "You can't use jetpack with a stack of items.");
				return;
			}
			if (!main.isUnique(item.getItem())) {
				main.setUnique(item.getItem());
			}

			p.openInventory(item.buildInventory(p));
		}
	}

	@EventHandler
	public void onSwapItems(PlayerSwapHandItemsEvent e) {
		Player p = e.getPlayer();
		JetpackItem item;
		if ((item = JetpackItem.fromItemStack(e.getOffHandItem())) != null) {
			item.setUsed(true);
			main.spawnJetpack(p, p.getVelocity());
			item.actualise(true);
		} else if ((item = JetpackItem.fromItemStack(e.getMainHandItem())) != null) {
			item.setUsed(false);
			main.killJetpack(p);
			item.actualise(true);
		}
	}
}
