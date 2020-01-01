package fr.seamoon.jetpack;

import java.util.Arrays;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemFlag;
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

public class JetpackMain extends JavaPlugin {
	
	private static JetpackMain instance;
	
	public ItemStack gasCylinderItem;
	
	@Override
	public void onLoad() {
		instance = this;
		
		{
			ItemStack item = new ItemStack(Material.BLAZE_ROD);
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Jetpack");
			meta.setLore(Arrays.asList(new String[]{
					(ChatColor.YELLOW + "Bouteille gauche : " + ChatColor.GOLD + "vide"),
					(ChatColor.YELLOW + "Bouteille droite : " + ChatColor.GOLD + "vide")
					}));
			PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(new NamespacedKey(this, "left"), PersistentDataType.FLOAT, -1f);
			data.set(new NamespacedKey(this, "right"), PersistentDataType.FLOAT, -1f);
			item.setItemMeta(meta);
			JetpackUtils.setJetpackItem(item);
		}
		{
			ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.GOLD + "Bouteille de gaz");
			meta.setLore(Arrays.asList(new String[]{ChatColor.YELLOW + "Puissance : " + ChatColor.GOLD + "100/100"}));
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.DIG_SPEED, 0, true);

			PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(new NamespacedKey(this, "power"), PersistentDataType.FLOAT, 100f);

			item.setItemMeta(meta);
			
			gasCylinderItem = item;
		}
	}
	
	@Override
	public void onEnable() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new OnFall(), this);
		getCommand("jetpack").setExecutor(new JetpackCommand());
		
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "jetpack"), JetpackUtils.jetpackItem);
		recipe.shape("*b*", "ifi");
		recipe.setIngredient('*', Material.AIR);
		recipe.setIngredient('b', Material.BLAZE_ROD);
		recipe.setIngredient('i', Material.ITEM_FRAME);
		recipe.setIngredient('f', Material.FLINT_AND_STEEL);
		getServer().addRecipe(recipe);
		
		FurnaceRecipe r = new FurnaceRecipe(new NamespacedKey(this, "gas_cylinder"), gasCylinderItem, Material.GLASS_BOTTLE, 10, 2000);
		getServer().addRecipe(r);
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				Player p = e.getPlayer();
				if (p.isInsideVehicle() && p.getVehicle() instanceof Item) {
					Item vehicle = (Item) p.getVehicle();
					JetpackItem item;
					if (vehicle.getType() == EntityType.DROPPED_ITEM && vehicle.getCustomName().equals("Jetpack") && (item = JetpackItem.fromItemStack(p.getInventory().getItemInOffHand())) != null) {
						if (item.getAmount() > 1) {
							p.sendMessage(ChatColor.RED + "You can't use jetpack with a stack of items.");
							return;
						}
						
						float z = e.getPacket().getFloat().read(1);
						boolean jump = e.getPacket().getBooleans().read(0);
						Vector to = vehicle.getVelocity().clone();
						boolean modif = false;
						if (z > 0 && !vehicle.isOnGround()) {
							Vector direction = p.getEyeLocation().getDirection();
							direction.setY(0);
							Vector v = to.clone().add(direction.normalize().multiply(0.2)).normalize().multiply(0.8);
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
									PlayerFlyWithJetpackEvent event = new PlayerFlyWithJetpackEvent(p, vehicle, item, vehicle.getLocation(), to);
									Bukkit.getPluginManager().callEvent(event);
									
									Vector finalVelocity = event.getVelocity();
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

	public static JetpackMain getInstance() {
		return instance;
	}
	
	/**
	 * Create a new jetpack and make player passenger
	 * @param p
	 * @param velocity 
	 */
	public void spawnJetpack(Player p, Vector velocity) {
		Item newItem = p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GLASS));
		newItem.setPickupDelay(Integer.MAX_VALUE);
		newItem.setInvulnerable(true);
		newItem.setCustomName("Jetpack");
		newItem.setCustomNameVisible(false);
		newItem.setVelocity(velocity);
		newItem.addPassenger(p);
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

	/**
	 * Return if the item is unique
	 * @param item
	 * @return
	 */
	public boolean isUnique(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();

		return data.has(new NamespacedKey(this, "created"), PersistentDataType.LONG);
	}

	/**
	 * Make the item unique : item can't be stacked
	 * @param item
	 */
	public void setUnique(ItemStack item) {
		setUnique(item, true);
	}
	
	/**
	 * Make the item unique or not : item can't be stacked
	 * @param currentItem
	 * @param b
	 */
	public void setUnique(ItemStack item, boolean unique) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		if (unique) {
			data.set(new NamespacedKey(this, "created"), PersistentDataType.LONG, new Date().getTime());
		} else if (data.has(new NamespacedKey(this, "created"), PersistentDataType.LONG)) {
			data.remove(new NamespacedKey(this, "created"));
		}
		item.setItemMeta(meta);
	}
	
	/**
	 * Return if passed item is a valid gas cylinder
	 * @param item
	 */
	public boolean isGasCylinderItem(ItemStack item) {
		if (item.getType().equals(gasCylinderItem.getType()) && item.getItemMeta().getDisplayName().equals(gasCylinderItem.getItemMeta().getDisplayName())) {
			ItemMeta meta = item.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			
			if (data.has(new NamespacedKey(this, "power"), PersistentDataType.FLOAT)) {
				return true;
			}
		}
		return false;
	}
}
