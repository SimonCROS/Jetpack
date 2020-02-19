package fr.seamoon.jetpack.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import fr.seamoon.jetpack.JetpackItem;
import fr.seamoon.jetpack.JetpackMain;
import fr.seamoon.jetpack.api.events.JetpackPacketReciveEvent;
import fr.seamoon.jetpack.api.events.PlayerFlyWithJetpackEvent;

public class OnJetpackPacketRecive implements Listener {

	private JetpackMain main = JetpackMain.getInstance();

	@EventHandler
	public void onPacketRecive(JetpackPacketReciveEvent e) {
		Player p = e.getPlayer();
		JetpackItem item = e.getItem();
		Item vehicle = e.getJetpack();
		if (item.getAmount() > 1) {
			p.sendMessage(ChatColor.RED + "You can't use jetpack with a stack of items.");
			return;
		}
		vehicle.setFallDistance((float) vehicle.getVelocity().getY() * 8 * -1);

		float x = e.getX();
		float z = e.getZ();
		boolean jump = e.isJumping();
		Vector to = vehicle.getVelocity().clone();
		boolean modif = false;

		if (x != 0 || z != 0 || jump) {
			if (!vehicle.isOnGround()) {
				Vector xyp = p.getEyeLocation().getDirection().setY(0).normalize().normalize();
				if (z > 0) {
					calc(to, xyp, 0.1, 1.2, 0);
				}
				if (z < 0) {
					calc(to, xyp, 0.05, 0.8, 0, true);
				}
				if (x > 0) {
					calc(to, xyp, 0.1, 1.2, 90);
				}
				if (x < 0) {
					calc(to, xyp, 0.1, 1.2, 270);
				}
			}
			if (jump) {
				to.setY(Math.min(0.6, to.getY() + 0.1));
			}
			p.getWorld().spawnParticle(Particle.REDSTONE, p.getLocation(), 10,
					new Particle.DustOptions(Color.fromBGR(255, 255, 255), 1));
			modif = true;
		}
		if (modif) {
			Vector finalVector = to;
			PlayerFlyWithJetpackEvent event = new PlayerFlyWithJetpackEvent(p, vehicle, item, vehicle.getLocation(),
					finalVector);
			Bukkit.getPluginManager().callEvent(event);

			Vector finalVelocity = event.getVelocity();
			if (!event.isCancelled()) {
				item.setFlying(true);
				item.actualise(false);
				vehicle.setVelocity(finalVelocity);
				long time = main.setUnique(item.getItem());
				BukkitRunnable flyinganimation = new BukkitRunnable() {

					@Override
					public void run() {
						if (time == main.getUniqueId(item.getItem())) {
							item.setFlying(false);
							item.actualise(false);
						}
					}
				};
				flyinganimation.runTaskLater(main, 3);
			}
		}
	}

	private void calc(Vector to, Vector xyp, double force, double max_speed, int rotation) {
		calc(to, xyp, force, max_speed, rotation, false);
	}

	private void calc(Vector to, Vector xyp, double force, double max_speed, int rotation, boolean reverse) {
		if (reverse) {
			force *= -1;
			max_speed *= -1;
		}
		Vector vec = to.clone().setY(0).add(xyp.multiply(force).rotateAroundY(Math.toRadians(rotation)));
		if ((reverse && vec.length() < max_speed) || (!reverse && vec.length() > max_speed)) {
			vec = vec.normalize().multiply(max_speed);
		}
		to.setX(vec.getX());
		to.setZ(vec.getZ());
	}
}
