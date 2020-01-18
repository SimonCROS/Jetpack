package fr.seamoon.jetpack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
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
import org.bukkit.scheduler.BukkitRunnable;
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
		pm.registerEvents(new OnFall(), this);
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
							vehicle.setFallDistance((float) vehicle.getVelocity().getY() * 8 * -1);
							JetpackItem item;
							if (vehicle.getType() == EntityType.DROPPED_ITEM
									&& vehicle.getCustomName().equals("Jetpack") && (item = JetpackItem
											.fromItemStack(p.getInventory().getItemInOffHand())) != null) {
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
									Vector v = to.clone().add(direction.normalize().multiply(0.2)).normalize()
											.multiply(0.8);
									to.setZ(v.getZ());
									to.setX(v.getX());
									modif = true;
								}
								if (jump) {
									to.setY(Math.min(0.6, to.getY() + 0.1));
									p.getWorld().spawnParticle(Particle.REDSTONE, p.getLocation(), 10,
											new Particle.DustOptions(Color.fromBGR(255, 255, 255), 1));

									modif = true;
								}
								if (modif) {
									plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
										@Override
										public void run() {
											PlayerFlyWithJetpackEvent event = new PlayerFlyWithJetpackEvent(p, vehicle,
													item, vehicle.getLocation(), to);
											Bukkit.getPluginManager().callEvent(event);

											Vector finalVelocity = event.getVelocity();
											if (!event.isCancelled()) {
												item.setFlying(true);
												item.actualise(false);
												vehicle.setVelocity(finalVelocity);
												long time = setUnique(item.getItem());
												BukkitRunnable flyinganimation = new BukkitRunnable() {

													@Override
													public void run() {
														if (time == getUniqueId(item.getItem())) {
															item.setFlying(false);
															item.actualise(false);
														}
													}
												};
												flyinganimation.runTaskLater(instance, 3);
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
			ItemMeta meta = item.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();

			if (data.has(new NamespacedKey(this, "power"), PersistentDataType.FLOAT)) {
				return true;
			}
		}
		return false;
	}
}
