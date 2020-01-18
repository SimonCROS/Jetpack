package fr.seamoon.jetpack;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ItemStackLib {

	private final ItemStack i;

	public ItemStackLib(Material material) {
		this(new ItemStack(material));
	}

	public ItemStackLib(ItemStack item) {
		i = item;
	}

	public Material getType() {
		return i.getType();
	}

	public ItemMeta getItemMeta() {
		return i.getItemMeta();
	}

	public ItemStackLib setItemMeta(ItemMeta itemMeta) {
		i.setItemMeta(itemMeta);
		return this;
	}

	public ItemStackLib setCustomModelData(Integer data) {
		ItemMeta m = getItemMeta();
		m.setCustomModelData(data);
		setItemMeta(m);
		return this;
	}

	public ItemStackLib setDisplayName(String name) {
		ItemMeta im = getItemMeta();
		im.setDisplayName(name);
		setItemMeta(im);
		return this;
	}

	public ItemStackLib setLore(String... lore) {
		return setLore(Arrays.asList(lore));
	}

	public ItemStackLib setLore(List<String> lore) {
		ItemMeta im = getItemMeta();
		im.setLore(lore);
		setItemMeta(im);
		return this;
	}

	public <T, Z> ItemStackLib set(Plugin plugin, String key, PersistentDataType<T, Z> type, Z value) {
		ItemMeta meta = getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		data.set(new NamespacedKey(plugin, key), type, value);
		i.setItemMeta(meta);
		return this;
	}

	public <T, Z> Z get(Plugin plugin, String key, PersistentDataType<T, Z> type) {
		ItemMeta meta = getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		return data.get(new NamespacedKey(plugin, key), type);
	}

	public <T, Z> boolean has(Plugin plugin, String key, PersistentDataType<T, Z> type) {
		ItemMeta meta = getItemMeta();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		return data.has(new NamespacedKey(plugin, key), type);
	}

	public ItemStackLib setAmount(int amount) {
		i.setAmount(amount);
		return this;
	}

	public ItemStackLib addItemFlags(ItemFlag flag) {
		ItemMeta im = getItemMeta();
		im.addItemFlags(flag);
		setItemMeta(im);
		return this;
	}

	public ItemStackLib removeItemFlags(ItemFlag flag) {
		ItemMeta im = getItemMeta();
		im.removeItemFlags(flag);
		setItemMeta(im);
		return this;
	}

	public ItemStack getItem() {
		return i;
	}

	public ItemStackLib setEnchantStyle() {
		ItemMeta im = getItemMeta();
		im.addEnchant(Enchantment.PIERCING, 0, true);
		im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		setItemMeta(im);
		return this;
	}
}
