package fr.seamoon.jetpack;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import org.bukkit.inventory.Inventory;
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
	public ItemStack jetpackItem;
	
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
			jetpackItem = item;
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
		
		
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "jetpack"), jetpackItem);
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
					if (vehicle.getType() == EntityType.DROPPED_ITEM && vehicle.getCustomName().equals("Jetpack") && isJetpackItem(p.getInventory().getItemInOffHand())) {
						ItemStack item = p.getInventory().getItemInOffHand();
						
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
									PlayerFlyWithJetpackEvent event = new PlayerFlyWithJetpackEvent(p, vehicle, item, vehicle.getLocation(), to, getJetpackLeftGasLevel(item), getJetpackRightGasLevel(item));
									Bukkit.getPluginManager().callEvent(event);
									
									Vector finalVelocity = event.getVelocity();
									setJetpackLeftGasLevel(item, event.getLeftGasLevel());
									setJetpackRightGasLevel(item, event.getRightGasLevel());
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
	 * Return if passed item is a valid jetpack
	 * @param item
	 */
	public boolean isJetpackItem(ItemStack item) {
		if (item != null && item.getType().equals(jetpackItem.getType()) && item.getItemMeta().getDisplayName().equals(jetpackItem.getItemMeta().getDisplayName())) {
			ItemMeta meta = item.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			
			if (data.has(new NamespacedKey(this, "left"), PersistentDataType.FLOAT) && data.has(new NamespacedKey(this, "right"), PersistentDataType.FLOAT)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the gasLevel of the jetpack
	 * @param jetpack
	 * @return -1 if the item is not valid, else, the gasLevel level
	 */
	public float getJetpackRightGasLevel(ItemStack jetpack) {
		return getJetpackGasLevel(jetpack, "right");
	}
	
	/**
	 * Set jetpack gasLevel to the passed value "gasLevel"
	 * @param jetpack
	 * @param gasLevel is an integer greater or equal than 0
	 */
	public void setJetpackRightGasLevel(ItemStack jetpack, float gasLevel) {
		setJetpackGasLevel(jetpack, gasLevel, "right");
	}
	
	/**
	 * Return the gasLevel of the jetpack
	 * @param jetpack
	 * @return -1 if the item is not valid, else, the gasLevel level
	 */
	public float getJetpackLeftGasLevel(ItemStack jetpack) {
		return getJetpackGasLevel(jetpack, "left");
	}
	
	/**
	 * Set jetpack gasLevel to the passed value "gasLevel"
	 * @param jetpack
	 * @param gasLevel is an integer greater or equal than 0
	 */
	public void setJetpackLeftGasLevel(ItemStack jetpack, float gasLevel) {
		setJetpackGasLevel(jetpack, gasLevel, "left");
	}

	private float getJetpackGasLevel(ItemStack jetpack, String side) {
		if (isJetpackItem(jetpack)) {
			ItemMeta meta = jetpack.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();

			return data.get(new NamespacedKey(this, side), PersistentDataType.FLOAT);
		}
		return -1;
	}

	private void setJetpackGasLevel(ItemStack jetpack, float gasLevel, String side) {
		if ((gasLevel >= 0 || gasLevel == -1) && isJetpackItem(jetpack)) {
			ItemMeta meta = jetpack.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(new NamespacedKey(this, side), PersistentDataType.FLOAT, gasLevel);
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
	
	Map<Material, Integer> gasPowerLevelColor = new HashMap<Material, Integer>() {
		private static final long serialVersionUID = 1L;
		{
	    put(Material.BLACK_STAINED_GLASS_PANE, 0);
	    put(Material.RED_STAINED_GLASS_PANE, 15);
	    put(Material.ORANGE_STAINED_GLASS_PANE, 40);
	    put(Material.YELLOW_STAINED_GLASS_PANE, 75);
	    put(Material.LIME_STAINED_GLASS_PANE, 100);
	}};

	/**
	 * Create inventory from jetpack item
	 * @param jetpack
	 * @return the inventory
	 */
	public Inventory buildJetpackInventory(Player p, ItemStack jetpack) {
		Inventory inventory = Bukkit.createInventory(p, 3*9, ChatColor.LIGHT_PURPLE + "Jetpack");
		inventory.setMaxStackSize(1);
		
		rebuildJetpackInventory(inventory, jetpack);

		return inventory;
	}
	
	/**
	 * Update inventory from jetpack item
	 * @param jetpack
	 */
	public void rebuildJetpackInventory(Inventory inventory, ItemStack jetpack) {
		inventory.clear();
		if (isJetpackItem(jetpack)) {
			ItemMeta emptyMeta = Bukkit.getItemFactory().getItemMeta(Material.GLASS_PANE);
			emptyMeta.setDisplayName(" ");
			
			ItemStack middle = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
			middle.setItemMeta(emptyMeta);
			for (int line = 0; line < 3; line++) {
				for (int index = 0; index < 3; index++) {
					inventory.setItem(line * 9 + index * 4, middle);
				}
			}
			float leftPower = getJetpackLeftGasLevel(jetpack);
			float rightPower = getJetpackRightGasLevel(jetpack);

			ItemStack left = new ItemStack(Material.GLASS_PANE);
			int lLast = 101;
			for (Entry<Material, Integer> material : gasPowerLevelColor.entrySet()) {
				if (material.getValue() < lLast && leftPower <= material.getValue()) {
					left = new ItemStack(material.getKey());
					lLast = material.getValue();
				}
			}
			left.setItemMeta(emptyMeta);

			ItemStack right = new ItemStack(Material.GLASS_PANE);
			int rLast = 101;
			for (Entry<Material, Integer> material : gasPowerLevelColor.entrySet()) {
				if (material.getValue() < rLast && rightPower <= material.getValue()) {
					right = new ItemStack(material.getKey());
					rLast = material.getValue();
				}
			}
			right.setItemMeta(emptyMeta);

			for (int line = 0; line < 3; line++) {
				for (int index = 1; index < 4; index++) {
					if (line == 1 && index == 2) continue;
					inventory.setItem(line * 9 + index, left);
				}
			}
			for (int line = 0; line < 3; line++) {
				for (int index = 5; index < 8; index++) {
					if (line == 1 && index == 6) continue;
					inventory.setItem(line * 9 + index, right);
				}
			}

			ItemStack rightItem = new ItemStack(Material.GLASS_BOTTLE);
			ItemMeta rightItemMeta = rightItem.getItemMeta();
			rightItemMeta.setDisplayName(ChatColor.RED + "Bouteille de gaz (ouverte)");
			rightItem.setItemMeta(rightItemMeta);
			setUnique(rightItem);
			
			if (leftPower >= 0) {
				ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.RED + "Bouteille de gaz (ouverte)");
				meta.setLore(Arrays.asList(new String[]{ChatColor.YELLOW + "Puissance : " + ChatColor.GOLD + (leftPower <= 0 ? "vide" : (Math.round(leftPower) + "/100"))}));
				item.setItemMeta(meta);
				setUnique(item);

				inventory.setItem(11, item);
			}
			
			if (rightPower >= 0) {
				ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.RED + "Bouteille de gaz (ouverte)");
				meta.setLore(Arrays.asList(new String[]{ChatColor.YELLOW + "Puissance : " + ChatColor.GOLD + (rightPower <= 0 ? "vide" : (Math.round(rightPower) + "/100"))}));
				item.setItemMeta(meta);
				setUnique(item);

				inventory.setItem(15, item);
			}
		}
	}

	/**
	 * Actualise lore of item
	 * @param item
	 */
	public void actualiseItem(ItemStack item) {
		if (isJetpackItem(item)) {
			float left = getJetpackLeftGasLevel(item);
			float right = getJetpackRightGasLevel(item);
			ItemMeta meta = item.getItemMeta();
			meta.setLore(Arrays.asList(new String[] {
					(ChatColor.YELLOW + "Bouteille gauche : " + ChatColor.GOLD + (left <= 0 ? "vide" : (Math.round(left) + "/100"))),
					(ChatColor.YELLOW + "Bouteille droite : " + ChatColor.GOLD + (right <= 0 ? "vide" : (Math.round(right) + "/100"))),
			}));
			item.setItemMeta(meta);
		}
	}
}
