package fr.seamoon.jetpack;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import fr.seamoon.jetpack.api.events.PlayerFlyWithJetpackEvent;
import fr.seamoon.jetpack.commands.JetpackCommand;
import fr.seamoon.jetpack.listeners.OnFall;
import net.md_5.bungee.api.ChatColor;

public class Jetpack extends JavaPlugin {
	
	private static Jetpack instance;
	public ItemStack jetpackItem;
	
	@Override
	public void onLoad() {
		instance = this;
		
		ItemStack item = new ItemStack(Material.BLAZE_ROD);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Jetpack");
		meta.setLore(Arrays.asList(new String[]{ChatColor.YELLOW + "Fuel : " + ChatColor.GOLD + "100/100"}));
		PersistentDataContainer data = meta.getPersistentDataContainer();
		data.set(new NamespacedKey(this, "power"), PersistentDataType.DOUBLE, 100d);
		item.setItemMeta(meta);
		jetpackItem = item;
	}
	
	@Override
	public void onEnable() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new OnFall(), this);
		getCommand("jetpack").setExecutor(new JetpackCommand());
		
		
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "jetpack"), jetpackItem);
		recipe.shape("*v*", "bIb", "f*f");
		recipe.setIngredient('*', Material.AIR);
		recipe.setIngredient('v', Material.STICKY_PISTON);
		recipe.setIngredient('I', Material.BLAZE_ROD);
		recipe.setIngredient('b', Material.GLASS_BOTTLE);
		recipe.setIngredient('f', Material.BLAZE_POWDER);
		getServer().addRecipe(recipe);
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				Player p = e.getPlayer();
				if (p.isInsideVehicle() && p.getVehicle() instanceof Item) {
					Item vehicle = (Item) p.getVehicle();
					if (vehicle.getType() == EntityType.DROPPED_ITEM && isJetpackItem(p.getInventory().getItemInOffHand())) {
						ItemStack item = p.getInventory().getItemInOffHand();
						
						float z = e.getPacket().getFloat().read(1);
						boolean jump = e.getPacket().getBooleans().read(0);
						Vector to = vehicle.getVelocity().clone();
						boolean modif = false;
						if (z > 0 && !vehicle.isOnGround()) {
							Vector v = to.clone().add(p.getEyeLocation().getDirection().normalize().multiply(0.2)).normalize().multiply(0.8);
							to.setZ(v.getZ());
							to.setX(v.getX());
							modif = true;
						}
						if (jump) {
							to.setY(Math.min(0.6, to.getY() + 0.1));
							p.getWorld().spawnParticle(Particle.REDSTONE, p.getLocation(), 10, new Particle.DustOptions(Color.fromBGR(255, 255, 255), 1));
	
							modif = true;
						}
						if (modif) {
							plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
								@Override
								public void run() {
									PlayerFlyWithJetpackEvent event = new PlayerFlyWithJetpackEvent(p, vehicle, item, vehicle.getLocation(), to, getJetpackFuel(item));
									Bukkit.getPluginManager().callEvent(event);
									
									Vector finalVelocity = event.getVelocity();
									setJetpackFuel(item, event.getFuel());
									if (!event.isCancelled()) {
										vehicle.setVelocity(finalVelocity);
									}
								}
							});
						}
					}
				}
			}
            @Override
            public void onPacketSending(PacketEvent e) {
            	
            }
        });
	}

	public static Jetpack getInstance() {
		return instance;
	}
	
	/**
	 * Create a new jetpack and make player passenger
	 * @param p
	 */
	public void spawnJetpack(Player p) {
		Item newItem = p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GLASS));
		newItem.setPickupDelay(Integer.MAX_VALUE);
		newItem.setInvulnerable(true);
		newItem.setVelocity(new Vector(0,0,0));
		newItem.addPassenger(p);
	}
	
	/**
	 * Return if passed item is a valid jetpack
	 * @param item
	 */
	public boolean isJetpackItem(ItemStack item) {
		if (item.getType().equals(jetpackItem.getType()) && item.getItemMeta().getDisplayName().equals(item.getItemMeta().getDisplayName())) {
			ItemMeta meta = item.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			
			if (data.has(new NamespacedKey(this, "power"), PersistentDataType.DOUBLE)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the fuel of the jetpack
	 * @param jetpack
	 * @return -1 if the item is not valid, else, the fuel level
	 */
	public double getJetpackFuel(ItemStack jetpack) {
		if (isJetpackItem(jetpack)) {
			ItemMeta meta = jetpack.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();

			return data.get(new NamespacedKey(this, "power"), PersistentDataType.DOUBLE);
		}
		return -1;
	}
	
	/**
	 * Set jetpack fuel to the passed value "fuel"
	 * @param jetpack
	 * @param fuel is an integer greater or equal than 0
	 */
	public void setJetpackFuel(ItemStack jetpack, double fuel) {
		if (fuel >= 0 && isJetpackItem(jetpack)) {
			ItemMeta meta = jetpack.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(new NamespacedKey(this, "power"), PersistentDataType.DOUBLE, fuel);
			jetpack.setItemMeta(meta);
		}
	}

	/**
	 * Kill player jetpack
	 * @param p
	 */
	public void killJetpack(Player p) {
		if (p.isInsideVehicle() && p.getVehicle() instanceof Item) {
			Location loc = p.getVehicle().getLocation();
			loc.setPitch(p.getLocation().getPitch());
			loc.setYaw(p.getLocation().getYaw());
			Entity vehicle = p.getVehicle();
			vehicle.remove();
			p.teleport(loc.add(0, 1, 0));
		}
	}

}
