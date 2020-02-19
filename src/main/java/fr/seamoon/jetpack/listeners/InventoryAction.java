package fr.seamoon.jetpack.listeners;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import fr.seamoon.jetpack.JetpackItem;
import fr.seamoon.jetpack.JetpackMain;
import fr.seamoon.jetpack.JetpackUtils;

public class InventoryAction implements Listener {

	private JetpackMain main = JetpackMain.getInstance();

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			Player p = (Player) e.getWhoClicked();
			if (e.getView().getTitle().equals(ChatColor.LIGHT_PURPLE + "Jetpack")) {
				for (Entry<Integer, ItemStack> s : e.getNewItems().entrySet()) {
					inventoryAction(p, e, ClickType.UNKNOWN, e.getView(), s.getKey(), s.getValue(),
							e.getView().getItem(s.getKey()));
					if (e.isCancelled()) {
						break;
					}
				}
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			Player p = (Player) e.getWhoClicked();
			if (e.getView().getTitle().equals(ChatColor.LIGHT_PURPLE + "Jetpack")) {
				inventoryAction(p, e, e.getClick(), e.getView(), e.getRawSlot(), e.getCursor(), e.getCurrentItem());
			}
		}
	}

	public void inventoryAction(Player p, Cancellable event, ClickType click, InventoryView view, int rawSlot,
			ItemStack cursor, ItemStack current) {
		if (JetpackUtils.isJetpackItem(current) || JetpackUtils.isJetpackItem(cursor)) {
			event.setCancelled(true);
			p.sendMessage(ChatColor.RED + "Please do not handle jetpack in this inventory.");
			return;
		}
		// If it's the jetpack inventory
		if (rawSlot < view.getTopInventory().getSize()) {
			if ((rawSlot == 11 || rawSlot == 15)) {
				if (main.isGasCylinderItem(cursor)) {
					if ((cursor.getAmount() > 1 && !click.equals(ClickType.RIGHT))
							|| (current != null && current.getType() != Material.AIR)) {
						p.sendMessage(ChatColor.RED + "You can only put one gas cylinder here.");
						event.setCancelled(true);
						return;
					}
					ItemStack old = view.getItem(rawSlot + 9);
					if (old != null && old.getType() != Material.AIR) {
						p.sendMessage(ChatColor.RED + "You need to remove the old gas cylender.");
						event.setCancelled(true);
					}
				} else if (cursor != null && cursor.getType() != Material.AIR) {
					p.sendMessage(ChatColor.RED + "You can only put gas cylender");
					event.setCancelled(true);
				}
			} else if ((rawSlot == 20 || rawSlot == 24) && cursor != null && cursor.getType() != Material.AIR) {
				event.setCancelled(true);
			} else if (rawSlot != 20 && rawSlot != 24) {
				event.setCancelled(true);
			}
		} else {
			if (click.equals(ClickType.DOUBLE_CLICK) || click.equals(ClickType.SHIFT_LEFT)
					|| click.equals(ClickType.SHIFT_RIGHT)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCloseInventory(InventoryCloseEvent e) {
		if (e.getPlayer() instanceof Player) {
			Player p = (Player) e.getPlayer();
			JetpackItem item;
			if (e.getView().getTitle().equals(ChatColor.LIGHT_PURPLE + "Jetpack")
					&& (item = JetpackItem.fromItemStack(p.getInventory().getItemInMainHand())) != null) {
				ItemStack newLeft = e.getView().getItem(11);
				ItemStack newRight = e.getView().getItem(15);
				ItemStack oldLeft = e.getView().getItem(20);
				ItemStack oldRight = e.getView().getItem(24);
				if (oldLeft == null || oldLeft.getType() == Material.AIR) {
					if (newLeft != null && newLeft.getType() != Material.AIR) {
						item.setLeftGasLevel(100);
					} else {
						item.setLeftGasLevel(-1);
					}
				}

				if (oldRight == null || oldRight.getType() == Material.AIR) {
					if (newRight != null && newRight.getType() != Material.AIR) {
						item.setRightGasLevel(100);
					} else {
						item.setRightGasLevel(-1);
					}
				}
				item.actualise(true);
			}
		}
	}
}
