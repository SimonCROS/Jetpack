package fr.seamoon.jetpack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
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

import fr.seamoon.jetpack.api.events.JetpackPacketReciveEvent;
import fr.seamoon.jetpack.commands.JetpackCommand;
import fr.seamoon.jetpack.listeners.Interract;
import fr.seamoon.jetpack.listeners.InventoryAction;
import fr.seamoon.jetpack.listeners.OnFly;
import fr.seamoon.jetpack.listeners.OnJetpackPacketRecive;
import fr.seamoon.jetpack.listeners.Security;

public class JetpackMain extends JavaPlugin {

	private static JetpackMain instance;

	public ItemStackLib gasCylinderItem;

	@Override
	public void onLoad() {
		instance = this;

		{
			JetpackUtils.setJetpackItem(
					new ItemStackLib(Material.BLAZE_ROD).setDisplayName(ChatColor.LIGHT_PURPLE + "Jetpack")
							.setLore(ChatColor.YELLOW + "Bouteille gauche : " + ChatColor.GOLD + "vide",
									ChatColor.YELLOW + "Bouteille droite : " + ChatColor.GOLD + "vide")
							.set(this, "left", PersistentDataType.FLOAT, -1f)
							.set(this, "right", PersistentDataType.FLOAT, -1f).setCustomModelData(100));
		}
		{
			gasCylinderItem = new ItemStackLib(Material.GLASS_BOTTLE)
					.setDisplayName(ChatColor.GOLD + "Bouteille de gaz").setEnchantStyle();
		}
	}

	@Override
	public void onEnable() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new OnFly(), this);
		pm.registerEvents(new Security(), this);
		pm.registerEvents(new InventoryAction(), this);
		pm.registerEvents(new Interract(), this);
		pm.registerEvents(new OnJetpackPacketRecive(), this);
		getCommand("jetpack").setExecutor(new JetpackCommand());

		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "jetpack"), JetpackUtils.jetpackItem.getItem());
		recipe.shape("*b*", "ifi");
		recipe.setIngredient('*', Material.AIR);
		recipe.setIngredient('b', Material.BLAZE_ROD);
		recipe.setIngredient('i', Material.ITEM_FRAME);
		recipe.setIngredient('f', Material.FLINT_AND_STEEL);
		getServer().addRecipe(recipe);

		FurnaceRecipe r = new FurnaceRecipe(new NamespacedKey(this, "gas_cylinder"), gasCylinderItem.getItem(),
				Material.GLASS_BOTTLE, 10, 2000);
		getServer().addRecipe(r);

		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(
				new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
					@Override
					public void onPacketReceiving(PacketEvent e) {
						Player p = e.getPlayer();
						if (p.isInsideVehicle() && p.getVehicle() instanceof Item) {
							Item vehicle = (Item) p.getVehicle();
							JetpackItem item;
							if (vehicle.getType() == EntityType.DROPPED_ITEM
									&& vehicle.getCustomName().equals("Jetpack") && (item = JetpackItem
											.fromItemStack(p.getInventory().getItemInOffHand())) != null) {
								float x = e.getPacket().getFloat().read(0);
								float z = e.getPacket().getFloat().read(1);
								boolean jump = e.getPacket().getBooleans().read(0);
								plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
									@Override
									public void run() {
										JetpackPacketReciveEvent event = new JetpackPacketReciveEvent(p, vehicle, item,
												x, z, jump);
										Bukkit.getPluginManager().callEvent(event);
									}
								});
							}
						}
					}

					@Override
					public void onPacketSending(PacketEvent e) {
					}
				});
		extractPack();
	}

	private void extractPack() {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}
		File yourFile = new File(getDataFolder().getAbsoluteFile(), "Jetpack_resourcepack.zip");
		if (yourFile.exists())
			return;
		try {
			FileOutputStream out = new FileOutputStream(yourFile);

			InputStream in = getResource("Jetpack_resourcepack.zip");

			int read = -1;

			while ((read = in.read()) != -1)
				out.write(read);

			out.flush();
			out.close();
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JetpackMain getInstance() {
		return instance;
	}

	/**
	 * Create a new jetpack and make player passenger
	 * 
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
	 * 
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
	 * 
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
	 * 
	 * @param item
	 */
	public long setUnique(ItemStack item) {
		return setUnique(item, true);
	}

	/**
	 * Make the item unique or not : item can't be stacked
	 * 
	 * @param currentItem
	 * @param b
	 * @return the created key (date)
	 */
	public long setUnique(ItemStack item, boolean unique) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		long time = new Date().getTime();
		if (unique) {
			data.set(new NamespacedKey(this, "created"), PersistentDataType.LONG, time);
		} else if (data.has(new NamespacedKey(this, "created"), PersistentDataType.LONG)) {
			data.remove(new NamespacedKey(this, "created"));
		}
		item.setItemMeta(meta);
		return time;
	}

	/**
	 * Make the item unique or not : item can't be stacked
	 * 
	 * @param currentItem
	 * @param b
	 * @return the created key (date)
	 */
	public long getUniqueId(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		if (data.has(new NamespacedKey(this, "created"), PersistentDataType.LONG)) {
			return data.get(new NamespacedKey(this, "created"), PersistentDataType.LONG);
		}
		return -1;
	}

	/**
	 * Return if passed item is a valid gas cylinder
	 * 
	 * @param item
	 */
	public boolean isGasCylinderItem(ItemStack item) {
		if (item != null && item.getType().equals(gasCylinderItem.getType())
				&& item.getItemMeta().getDisplayName().equals(gasCylinderItem.getItemMeta().getDisplayName())) {
			return true;
		}
		return false;
	}
}
