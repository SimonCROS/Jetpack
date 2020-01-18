package fr.seamoon.jetpack.listeners;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;

import fr.seamoon.jetpack.ItemStackLib;
import fr.seamoon.jetpack.JetpackItem;
import fr.seamoon.jetpack.JetpackMain;
import fr.seamoon.jetpack.JetpackUtils;
import fr.seamoon.jetpack.api.events.PlayerFlyWithJetpackEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class OnFall implements Listener {

	private JetpackMain main = JetpackMain.getInstance();

//	@EventHandler
//	public void onJoin(PlayerJoinEvent e) {
//		
//	}

	@EventHandler
	public void onDismount(EntityDismountEvent e) {
		if (!e.getDismounted().isDead() && e.getEntity() instanceof Player && e.getDismounted() instanceof Item
				&& e.getDismounted().getCustomName().equals("Jetpack")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onSelectItem(PlayerSwapHandItemsEvent e) {
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

	@EventHandler
	public void onRespawn(PlayerDeathEvent e) {
		if (e.getEntity() instanceof Player) {
			Player p = e.getEntity();
			if (p.isInsideVehicle() && p.getVehicle() instanceof Item) {
				p.getVehicle().remove();
				ItemStackLib item = new ItemStackLib(p.getInventory().getItemInOffHand());
				if (JetpackUtils.isJetpackItem(item.getItem())) {
					item.setCustomModelData(100);
				}
			}
		}
	}

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
				}
			} else if ((rawSlot == 20 || rawSlot == 24) && (cursor == null || cursor.getType() == Material.AIR)) {

			} else {
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
