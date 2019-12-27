package fr.seamoon.jetpack.listeners;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

import fr.seamoon.jetpack.Jetpack;
import fr.seamoon.jetpack.api.events.PlayerFlyWithJetpackEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class OnFall implements Listener {
	
	private Jetpack jetpack = Jetpack.getInstance();

	@EventHandler
	public void onFall(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FALL && e.getEntity() instanceof Player && e.getEntity().isInsideVehicle() && e.getEntity().getVehicle() instanceof Item) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDismount(EntityDismountEvent e) {
		if (!e.getDismounted().isDead() && e.getEntity() instanceof Player && e.getDismounted() instanceof Item) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onSelectItem(PlayerSwapHandItemsEvent e) {
		Player p = e.getPlayer();
		if (jetpack.isJetpackItem(e.getOffHandItem())) {
			jetpack.spawnJetpack(p);
		} else if (jetpack.isJetpackItem(e.getMainHandItem())) {
			jetpack.killJetpack(p);
		}
	}

	@EventHandler
	public void onFly(PlayerFlyWithJetpackEvent e) {
		Player p = e.getPlayer();
		int bars = 100;
		int redbars = (int) Math.round(bars * e.getFuel() / 100);
		int emptyBars = bars - redbars;
		String message = "§4" + new String(new char[redbars]).replace("\0", "|") + "§f" + new String(new char[emptyBars]).replace("\0", "|");
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
		if (e.getFuel() <= 0) {
			e.setCancelled(true);
		} else {
			e.setFuel(e.getFuel() - 0.1);
		}
	}
}
