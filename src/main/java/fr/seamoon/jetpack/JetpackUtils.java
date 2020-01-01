package fr.seamoon.jetpack;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class JetpackUtils {
	
	private static JetpackMain main = JetpackMain.getInstance();

	public static JetpackItem jetpackItem;
	
	/**
	 * Return if passed item is a valid jetpack
	 * @param item
	 */
	public static boolean isJetpackItem(ItemStack item) {
		if (item != null && item.getType().equals(jetpackItem.getType()) && item.getItemMeta().getDisplayName().equals(jetpackItem.getItemMeta().getDisplayName())) {
			ItemMeta meta = item.getItemMeta();
			PersistentDataContainer data = meta.getPersistentDataContainer();
			
			if (data.has(new NamespacedKey(main, "left"), PersistentDataType.FLOAT) && data.has(new NamespacedKey(main, "right"), PersistentDataType.FLOAT)) {
				return true;
			}
		}
		return false;
	}

	public static void setJetpackItem(ItemStack item) {
		jetpackItem = new JetpackItem(item);
	}

	public static JetpackItem getJetpackItem(ItemStack item) {
		return jetpackItem;
	}

}
