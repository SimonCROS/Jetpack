package fr.seamoon.jetpack;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class JetpackItem {

	private static JetpackMain main = JetpackMain.getInstance();
	private final ItemStack i;

	Map<Material, Integer> gasPowerLevelColor = new HashMap<Material, Integer>() {
		private static final long serialVersionUID = 1L;
		{
			put(Material.BARRIER, -1);
			put(Material.BLACK_STAINED_GLASS_PANE, 0);
			put(Material.RED_STAINED_GLASS_PANE, 15);
			put(Material.ORANGE_STAINED_GLASS_PANE, 40);
			put(Material.YELLOW_STAINED_GLASS_PANE, 75);
			put(Material.LIME_STAINED_GLASS_PANE, 100);
		}
	};
	private boolean used;
	private boolean flying;

	public int getAmount() {
		return i.getAmount();
	}

	public ItemMeta getItemMeta() {
		return i.getItemMeta();
	}

	public boolean setItemMeta(ItemMeta itemMeta) {
		return i.setItemMeta(itemMeta);
	}

	public Material getType() {
		return i.getType();
	}

	public ItemStack getItem() {
		return i;
	}

	/**
	 * Return new jetpack item from itemstack
	 * 
	 * @param item
	 * @return the item if it is a jetpack, else return null
	 */
	public static JetpackItem fromItemStack(ItemStack item) {
		if (JetpackUtils.isJetpackItem(item)) {
			return new JetpackItem(item);
		}
		return null;
	}

	public JetpackItem(ItemStackLib item) {
		this(item.getItem());
	}

	/**
	 * Use JetpackUtils.fromItemStack(ItemStack item) to instantiate
	 * 
	 * @param item
	 */
	public JetpackItem(ItemStack item) {
		i = item;
		if (getItemMeta().hasCustomModelData())
			used = getItemMeta().getCustomModelData() != 100;
	}

	/**
	 * Return the gasLevel of the jetpack
	 * 
	 * @param jetpack
	 * @return -1 if the item is not valid, else, the gasLevel level
	 */
	public float getRightGasLevel() {
		return getGasLevel("right");
	}

	/**
	 * Set jetpack gasLevel to the passed value "gasLevel"
	 * 
	 * @param jetpack
	 * @param gasLevel is an integer greater or equal than 0
	 */
	public void setRightGasLevel(float gasLevel) {
		setGasLevel(gasLevel, "right");
	}

	/**
	 * Return the gasLevel of the jetpack
	 * 
	 * @param jetpack
	 * @return -1 if the item is not valid, else, the gasLevel level
	 */
	public float getLeftGasLevel() {
		return getGasLevel("left");
	}

	/**
	 * Set jetpack gasLevel to the passed value "gasLevel"
	 * 
	 * @param jetpack
	 * @param gasLevel is an integer greater or equal than 0
	 */
	public void setLeftGasLevel(float gasLevel) {
		setGasLevel(gasLevel, "left");
	}

	private float getGasLevel(String side) {
		PersistentDataContainer data = getItemMeta().getPersistentDataContainer();
		if (data.has(new NamespacedKey(main, side), PersistentDataType.FLOAT)) {
			return data.get(new NamespacedKey(main, side), PersistentDataType.FLOAT);
		}
		return -1;
	}

	private void setGasLevel(float gasLevel, String side) {
		gasLevel = new BigDecimal(gasLevel).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
		if (gasLevel >= 0 || gasLevel == -1) {
			ItemMeta meta = getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(new NamespacedKey(main, side), PersistentDataType.FLOAT, gasLevel);
			i.setItemMeta(meta);
		}
	}

	/**
	 * Create inventory from jetpack item
	 * 
	 * @param jetpack
	 * @return the inventory
	 */
	public Inventory buildInventory(Player p) {
		Inventory inventory = Bukkit.createInventory(p, 3 * 9, ChatColor.LIGHT_PURPLE + "Jetpack");
		inventory.setMaxStackSize(1);

		rebuildInventory(inventory);

		return inventory;
	}

	/**
	 * Update inventory from jetpack item
	 * 
	 * @param inventory the inventory to rebuild
	 */
	public void rebuildInventory(Inventory inventory) {
		inventory.clear();

		ItemMeta emptyMeta = Bukkit.getItemFactory().getItemMeta(Material.GLASS_PANE);
		emptyMeta.setDisplayName(" ");

		ItemStackLib middle = new ItemStackLib(Material.WHITE_STAINED_GLASS_PANE).setItemMeta(emptyMeta);
		for (int line = 0; line < 3; line++) {
			for (int index = 0; index < 3; index++) {
				inventory.setItem(line * 9 + index * 4, middle.getItem());
			}
		}

		float leftPower = getLeftGasLevel();
		float rightPower = getRightGasLevel();

		ItemStackLib left = getSideItem(leftPower).setItemMeta(emptyMeta);
		ItemStackLib right = getSideItem(rightPower).setItemMeta(emptyMeta);

		for (int line = 0; line < 3; line++) {
			for (int index = 1; index < 4; index += 2) {
				inventory.setItem(line * 9 + index, left.getItem());
			}
		}
		for (int line = 0; line < 3; line++) {
			for (int index = 5; index < 8; index += 2) {
				inventory.setItem(line * 9 + index, right.getItem());
			}
		}

		ItemStack bottleItem = new ItemStack(Material.GLASS_BOTTLE);
		ItemMeta rightItemMeta = bottleItem.getItemMeta();
		rightItemMeta.setDisplayName(ChatColor.RED + "Bouteille de gaz (ouverte)");
		bottleItem.setItemMeta(rightItemMeta);
		main.setUnique(bottleItem);

		ItemStackLib leftSign = new ItemStackLib(Material.OAK_SIGN)
				.setDisplayName(ChatColor.LIGHT_PURPLE + "Bouteille gauche")
				.setLore(ChatColor.GOLD + gasLevelToString("left"));
		ItemStackLib rightSign = new ItemStackLib(Material.OAK_SIGN)
				.setDisplayName(ChatColor.LIGHT_PURPLE + "Bouteille droite")
				.setLore(ChatColor.GOLD + gasLevelToString("right"));

		ItemStackLib item = new ItemStackLib(Material.GLASS_BOTTLE)
				.setDisplayName(ChatColor.RED + "Bouteille de gaz (ouverte)");

		inventory.setItem(2, leftSign.getItem());
		if (leftPower >= 0)
			inventory.setItem(20, item.getItem());

		inventory.setItem(6, rightSign.getItem());
		if (rightPower >= 0)
			inventory.setItem(24, item.getItem());
	}

	private ItemStackLib getSideItem(float rightPower) {
		ItemStackLib item = new ItemStackLib(Material.GLASS_PANE);
		int last = 102;
		for (Entry<Material, Integer> material : gasPowerLevelColor.entrySet()) {
			if (material.getValue() < last && rightPower <= material.getValue()) {
				item = new ItemStackLib(material.getKey());
				last = material.getValue();
			}
		}
		return item;
	}

	private String gasLevelToString(String key) {
		float level = getGasLevel(key);
		if (level < 0)
			return "Pas de bouteille";
		else if (level == 0)
			return "Vide";
		else
			return Math.round(level) + "/100";
	}

	public boolean isFlying() {
		return flying;
	}

	public void setFlying(boolean flying) {
		this.flying = flying;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	/**
	 * Actualise item
	 * 
	 * @param force
	 */
	public void actualise(boolean force) {
		ItemMeta meta = getItemMeta();
		float left = getLeftGasLevel();
		float right = getRightGasLevel();
		if (force) {
			meta.setLore(Arrays.asList(new String[] {
					(ChatColor.YELLOW + "Bouteille gauche : " + ChatColor.GOLD
							+ (left <= 0 ? "vide" : (Math.round(left) + "/100"))),
					(ChatColor.YELLOW + "Bouteille droite : " + ChatColor.GOLD
							+ (right <= 0 ? "vide" : (Math.round(right) + "/100"))), }));
		}

		// Model data doesn't support 0
		int model = used ? flying ? 102 : 101 : 100;
		if (force || meta.getCustomModelData() != model) {
			meta.setCustomModelData(model);
			i.setItemMeta(meta);
		}
	}
}
