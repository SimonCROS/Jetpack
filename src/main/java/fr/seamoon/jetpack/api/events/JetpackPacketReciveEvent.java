package fr.seamoon.jetpack.api.events;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.seamoon.jetpack.JetpackItem;

public class JetpackPacketReciveEvent extends Event implements Cancellable {

	private boolean isCancelled;
	private Player player;
	private Item jetpack;
	private JetpackItem item;
	private float x;
	private float z;
	private boolean jump;
	private static final HandlerList handlers = new HandlerList();

	public JetpackPacketReciveEvent(Player p, Item vehicle, JetpackItem item, float x, float z, boolean jump) {
		this.player = p;
		this.jetpack = vehicle;
		this.item = item;
		this.x = x;
		this.z = z;
		this.jump = jump;
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

	public JetpackItem getItem() {
		return item;
	}

	public float getX() {
		return x;
	}

	public float getZ() {
		return z;
	}

	public boolean isJumping() {
		return jump;
	}
}