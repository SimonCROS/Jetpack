package fr.seamoon.jetpack.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

import fr.seamoon.jetpack.JetpackItem;

public class PlayerFlyWithJetpackEvent extends Event implements Cancellable {

	private boolean isCancelled;
	private Player player;
	private Item jetpack;
	private Location from;
	private Vector velocity;
	private JetpackItem item;
	private static final HandlerList handlers = new HandlerList();

	public PlayerFlyWithJetpackEvent(Player p, Item vehicle, JetpackItem item, Location location, Vector to) {
		this.player = p;
		this.jetpack = vehicle;
		this.from = location;
		this.item = item;
		this.setVelocity(to);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return this.isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public Player getPlayer() {
		return player;
	}

	public Item getJetpack() {
		return jetpack;
	}

	public Location getFrom() {
		return from;
	}

	public Vector getVelocity() {
		return velocity;
	}

	public void setVelocity(Vector velocity) {
		this.velocity = velocity;
	}

	public JetpackItem getItem() {
		return item;
	}
}
