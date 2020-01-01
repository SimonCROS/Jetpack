package fr.seamoon.jetpack.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spigotmc.event.entity.EntityDismountEvent;

import fr.seamoon.jetpack.JetpackItem;
import fr.seamoon.jetpack.JetpackMain;
import fr.seamoon.jetpack.JetpackUtils;
import fr.seamoon.jetpack.api.events.PlayerFlyWithJetpackEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class OnFall implements Listener {
	
	private JetpackMain main = JetpackMain.getInstance();

	@EventHandler
	public void onDismount(EntityDismountEvent e) {
		if (!e.getDismounted().isDead() && e.getEntity() instanceof Player && e.getDismounted() instanceof Item && e.getDismounted().getCustomName().equals("Jetpack")) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onSelectItem(PlayerSwapHandItemsEvent e) {
		Player p = e.getPlayer();
		JetpackItem item;
		if (JetpackUtils.isJetpackItem(e.getOffHandItem())) {
			main.spawnJetpack(p, p.getVelocity());
		} else if ((item = JetpackItem.fromItemStack(e.getMainHandItem())) != null) {
			main.killJetpack(p);
			item.actualise();
		}
	}

	@EventHandler
	public void onFly(PlayerFlyWithJetpackEvent e) {
		Player p = e.getPlayer();
		JetpackItem item = e.getItem();
		float leftLevel = item.getLeftGasLevel();
		float rightLevel = item.getRightGasLevel();
		int bars = 50;

		int leftRedBars = (int) Math.round(bars * leftLevel / 100);
		int leftEmptyBars = bars - leftRedBars;
		
		int rightRedBars = (int) Math.round(bars * rightLevel / 100);
		int rightEmptyBars = bars - rightRedBars;
		
		String left = Math.round(leftLevel) + "�4 " + new String(new char[leftRedBars]).replace("\0", "|") + "�f" + new String(new char[leftEmptyBars]).replace("\0", "|");
		String right = Math.round(rightLevel) + "�4 " + new String(new char[rightRedBars]).replace("\0", "|") + "�f" + new String(new char[rightEmptyBars]).replace("\0", "|");
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(left + "    �r" + right));

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
	public void clickEvent(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		JetpackItem item;
		if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && e.getHand().equals(EquipmentSlot.HAND) && (item = JetpackItem.fromItemStack(e.getItem())) != null) {
			if (item.getAmount() > 1) {
				p.sendMessage(ChatColor.RED + "You can't use jetpack with a stack of items.");
				return;
			}
			if (!main.isUnique(item)) {
				main.setUnique(item);
			}
			
			p.openInventory(item.buildInventory(p));
		}
	}
	
	@EventHandler
	public void inventoryEvent(InventoryDragEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (e.getView().getTitle().equals(ChatColor.LIGHT_PURPLE + "Jetpack")) {
				e.setCancelled(true);
			}
		}
	}
		
	@EventHandler
	public void clickEvent(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
//			Player p = (Player) e.getWhoClicked();
//			ItemStack item = p.getInventory().getItemInMainHand();
//			if (e.getView().getTitle().equals(ChatColor.LIGHT_PURPLE + "Jetpack")) {
//				if (main.isJetpackItem(e.getCurrentItem()) || main.isJetpackItem(e.getCursor())) {
//					e.setCancelled(true);
//					p.sendMessage(ChatColor.RED + "Please do not handle jetpack in this inventory.");
//					return;
//				}
//				if (e.getRawSlot() == e.getSlot()) {
//					if (e.getRawSlot() == 11 || e.getRawSlot() == 15) {
//						if (main.isGasCylinderItem(e.getCursor())) {
//							if (e.getCursor().getAmount() > 1) {
//								p.sendMessage(ChatColor.RED + "You can only put one gas cylinder here.");
//								e.setCancelled(true);
//								return;
//							}
//							if (e.getRawSlot() == 11) {
//								main.setJetpackLeftGasLevel(item, 100f);
//							} else {
//								main.setJetpackRightGasLevel(item, 100f);
//							}
//							BukkitRunnable runnable = new BukkitRunnable() {
//								
//								@Override
//								public void run() {
//									main.rebuildJetpackInventory(e.getInventory(), item);
//									p.updateInventory();
//								}
//							};
//							runnable.runTaskLater(main, 5);
//						} else if (e.getCursor().getType() == Material.AIR) {
//							if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.GLASS_BOTTLE) {
//								main.setUnique(e.getCurrentItem(), false);
//								if (e.getRawSlot() == 11) {
//									main.setJetpackLeftGasLevel(item, -1f);
//								} else {
//									main.setJetpackRightGasLevel(item, -1f);
//								}
//								BukkitRunnable runnable = new BukkitRunnable() {
//									
//									@Override
//									public void run() {
//										main.rebuildJetpackInventory(e.getInventory(), item);
//										p.updateInventory();
//									}
//								};
//								runnable.runTaskLater(main, 5);
//							}
//						} else {
//							e.setCancelled(true);
//						}
//					} else {
//						e.setCancelled(true);
//					}
//				} else {
//					if (e.getClick().equals(ClickType.DOUBLE_CLICK) || e.getClick().equals(ClickType.SHIFT_LEFT) || e.getClick().equals(ClickType.SHIFT_RIGHT)) {
//						e.setCancelled(true);
//					}
//				}
//			}
		}
	}
	
	@EventHandler
	public void closeInventory(InventoryCloseEvent e) {
		if (e.getPlayer() instanceof Player) {
			Player p = (Player) e.getPlayer();
			JetpackItem item;
			if (e.getView().getTitle().equals(ChatColor.LIGHT_PURPLE + "Jetpack") && (item = JetpackItem.fromItemStack(p.getInventory().getItemInMainHand())) != null) {
				item.actualise();
			}
		}
	}
}
