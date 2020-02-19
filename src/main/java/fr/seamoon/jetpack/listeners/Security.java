package fr.seamoon.jetpack.listeners;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

import fr.seamoon.jetpack.ItemStackLib;
import fr.seamoon.jetpack.JetpackUtils;

public class Security implements Listener {

	@EventHandler
	public void onKick(PlayerKickEvent e) {
		if (e.getReason().equalsIgnoreCase("Flying is not enabled on this server")
				&& JetpackUtils.isJetpackItem(e.getPlayer().getInventory().getItemInOffHand())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.SUFFOCATION && e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			if (JetpackUtils.isJetpackItem(p.getInventory().getItemInOffHand())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onDismount(EntityDismountEvent e) {
		if (!e.getDismounted().isDead() && e.getEntity() instanceof Player && e.getDismounted() instanceof Item
				&& e.getDismounted().getCustomName().equals("Jetpack")) {
			e.setCancelled(true);
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
}
