package protocolsupport.zplatform.impl.spigot.entitytracker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.server.v1_12_R1.DataWatcher;
import net.minecraft.server.v1_12_R1.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.spigotmc.AsyncCatcher;

import net.minecraft.server.v1_12_R1.AttributeInstance;
import net.minecraft.server.v1_12_R1.AttributeMapServer;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityAreaEffectCloud;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import net.minecraft.server.v1_12_R1.EntityArrow;
import net.minecraft.server.v1_12_R1.EntityBoat;
import net.minecraft.server.v1_12_R1.EntityDragonFireball;
import net.minecraft.server.v1_12_R1.EntityEgg;
import net.minecraft.server.v1_12_R1.EntityEnderCrystal;
import net.minecraft.server.v1_12_R1.EntityEnderPearl;
import net.minecraft.server.v1_12_R1.EntityEnderSignal;
import net.minecraft.server.v1_12_R1.EntityEvokerFangs;
import net.minecraft.server.v1_12_R1.EntityExperienceOrb;
import net.minecraft.server.v1_12_R1.EntityFallingBlock;
import net.minecraft.server.v1_12_R1.EntityFireball;
import net.minecraft.server.v1_12_R1.EntityFireworks;
import net.minecraft.server.v1_12_R1.EntityFishingHook;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityItem;
import net.minecraft.server.v1_12_R1.EntityItemFrame;
import net.minecraft.server.v1_12_R1.EntityLeash;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.EntityLlamaSpit;
import net.minecraft.server.v1_12_R1.EntityMinecartAbstract;
import net.minecraft.server.v1_12_R1.EntityPainting;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.EntityPotion;
import net.minecraft.server.v1_12_R1.EntityShulkerBullet;
import net.minecraft.server.v1_12_R1.EntitySmallFireball;
import net.minecraft.server.v1_12_R1.EntitySnowball;
import net.minecraft.server.v1_12_R1.EntitySpectralArrow;
import net.minecraft.server.v1_12_R1.EntityTNTPrimed;
import net.minecraft.server.v1_12_R1.EntityThrownExpBottle;
import net.minecraft.server.v1_12_R1.EntityTippedArrow;
import net.minecraft.server.v1_12_R1.EntityTrackerEntry;
import net.minecraft.server.v1_12_R1.EntityTypes;
import net.minecraft.server.v1_12_R1.EntityWitherSkull;
import net.minecraft.server.v1_12_R1.EnumItemSlot;
import net.minecraft.server.v1_12_R1.IAnimal;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.ItemWorldMap;
import net.minecraft.server.v1_12_R1.Items;
import net.minecraft.server.v1_12_R1.MathHelper;
import net.minecraft.server.v1_12_R1.MobEffect;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayOutBed;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntity;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityEffect;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_12_R1.PacketPlayOutMount;
import net.minecraft.server.v1_12_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityExperienceOrb;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityPainting;
import net.minecraft.server.v1_12_R1.PacketPlayOutUpdateAttributes;
import net.minecraft.server.v1_12_R1.WorldMap;
import protocolsupport.utils.CachedInstanceOfChain;

public class SpigotEntityTrackerEntry extends EntityTrackerEntry {

	protected static final boolean paperTrackedPlayersMapPresent = checkPaperTrackedPlayersMap();
	protected static final boolean checkPaperTrackedPlayersMap() {
		try {
			EntityTrackerEntry.class.getDeclaredField("trackedPlayerMap");
			return true;
		} catch (NoSuchFieldException | SecurityException e) {
			return false;
		}
	}

	private final Entity tracker;
	private final EntityLiving trackerLiving;
	private final Set<AttributeInstance> attributeSet;

	private int viewDistance;
	private int trackingRange;
	private int updateInterval;
	private boolean updateVelocity;

	private double lastLocationX;
	private double lastLocationY;
	private double lastLocationZ;

	private double lastMotionX;
	private double lastMotionY;
	private double lastMotionZ;

	private float trackerYaw;
	private float trackerPitch;
	private float trackerHeadYaw;

	private List<Entity> passengerList;

	private boolean trackerMoving;

	public SpigotEntityTrackerEntry(Entity entity, int trackRange, int viewDistance, int updateInterval, boolean updateVelocity) {
		super(entity, trackRange, viewDistance, updateInterval, updateVelocity);

		this.tracker = entity;
		this.trackerLiving = (entity instanceof EntityLiving) ? (EntityLiving) entity : null;

		this.passengerList = Collections.emptyList();

		if (entity instanceof EntityLiving) {
			this.attributeSet = ((AttributeMapServer) ((EntityLiving) entity).getAttributeMap()).getAttributes();
		} else {
			this.attributeSet = Collections.emptySet();
		}

		this.viewDistance = viewDistance;
		this.trackingRange = trackRange;
		this.updateInterval = updateInterval;
		this.updateVelocity = updateVelocity;

		this.trackerYaw = MathHelper.d((entity.yaw * 256.0F) / 360.0F);
		this.trackerPitch = MathHelper.d((entity.pitch * 256.0F) / 360.0F);
		this.trackerHeadYaw = MathHelper.d((entity.getHeadRotation() * 256.0F) / 360.0F);
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (!object.getClass().equals(this.getClass())) {
			return false;
		}
		return (((SpigotEntityTrackerEntry) object).tracker.getId() == tracker.getId());
	}

	@Override
	public int hashCode() {
		return this.tracker.getId();
	}

	protected void updateRotationIfChanged() {
		byte yaw = (byte) MathHelper.d((this.tracker.yaw * 256.0F) / 360.0F);
		byte pitch = (byte) MathHelper.d((this.tracker.pitch * 256.0F) / 360.0F);
		if ((Math.abs(yaw - this.trackerYaw) >= 1) || (Math.abs(pitch - this.trackerPitch) >= 1)) {
			this.broadcast(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.tracker.getId(), yaw, pitch, this.tracker.onGround));
			this.trackerYaw = yaw;
			this.trackerPitch = pitch;
		}
	}

	@Override
	public void track(List<EntityHuman> worldPlayers) {
		this.b = false;

		if (!this.trackerMoving || this.tracker.d(this.lastLocationX, this.lastLocationY, this.lastLocationZ) > 16.0) {
			this.lastLocationX = this.tracker.locX;
			this.lastLocationY = tracker.locY;
			this.lastLocationZ = tracker.locZ;
			this.trackerMoving = true;
			this.b = true;
			scanPlayers(worldPlayers);
		}

		List<Entity> passengers = this.tracker.bF();

		if (!passengers.equals(this.passengerList)) {
			this.passengerList = passengers;
			broadcastIncludingSelf(new PacketPlayOutMount(this.tracker));
		}

		if (this.tracker instanceof EntityItemFrame && this.a % 20 == 0) {
			EntityItemFrame entityItemFrame = (EntityItemFrame) this.tracker;
			ItemStack itemStack = entityItemFrame.getItem();

			if (itemStack.getItem() instanceof ItemWorldMap) {
				WorldMap worldMap = Items.FILLED_MAP.getSavedMap(itemStack, this.tracker.world);

				for (EntityHuman entityHuman : this.trackedPlayers) {
					EntityPlayer entityPlayer = (EntityPlayer) entityHuman;

					worldMap.a(entityPlayer, itemStack);

					Packet packet = Items.FILLED_MAP.a(itemStack, this.tracker.world, entityHuman);
					if (packet != null) {
						entityPlayer.playerConnection.sendPacket(packet);
					}
				}
			}

			this.updateMetadataAndAttributes();
		}

		if (this.a > 0 && (this.a % this.updateInterval == 0 || this.tracker.impulse || this.tracker.getDataWatcher().a())) {
			this.tracker.impulse = false;

			if (this.tracker.isPassenger()) {
				this.updateRotationIfChanged();
			} else {
				if (
					Math.abs(this.tracker.locX - this.lastLocationX) >= 0.03125D ||
					Math.abs(this.tracker.locY - this.lastLocationY) >= 0.015625D ||
					Math.abs(this.tracker.locZ - this.lastLocationZ) >= 0.03125D
				) {
					this.lastLocationX = this.tracker.locX;
					this.lastLocationY = this.tracker.locY;
					this.lastLocationZ = this.tracker.locZ;

					this.broadcast(new PacketPlayOutEntityTeleport(this.tracker));
				} else {
					this.updateRotationIfChanged();
				}

				if (this.updateVelocity) {
					double diffMotX = this.tracker.motX - this.lastMotionX;
					double diffMotY = this.tracker.motY - this.lastMotionY;
					double diffMotZ = this.tracker.motZ - this.lastMotionZ;
					double diffMot = (diffMotX * diffMotX) + (diffMotY * diffMotY) + (diffMotZ * diffMotZ);

					if ((diffMot > 4.0E-4) || ((diffMot > 0.0) && (this.tracker.motX == 0.0) && (this.tracker.motY == 0.0) && (this.tracker.motZ == 0.0))) {
						this.lastMotionX = this.tracker.motX;
						this.lastMotionY = this.tracker.motY;
						this.lastMotionZ = this.tracker.motZ;

						broadcast(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.lastMotionX, this.lastMotionY, this.lastMotionZ));
					}
				}

				float eHeadYaw = this.tracker.getHeadRotation();
				if (Math.abs(eHeadYaw - this.trackerHeadYaw) >= 1) {
					this.trackerHeadYaw = eHeadYaw;
					broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte) MathHelper.d((eHeadYaw * 256.0f) / 360.0f)));
				}
			}

			this.updateMetadataAndAttributes();
		}

		++this.a;

		if (this.tracker.velocityChanged) {
			boolean cancelled = false;

			if (this.tracker instanceof EntityPlayer) {
				Player player = (Player) this.tracker.getBukkitEntity();
				org.bukkit.util.Vector velocity = player.getVelocity();

				PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
				this.tracker.world.getServer().getPluginManager().callEvent(event);

				if (event.isCancelled()) {
					cancelled = true;
				} else if (!velocity.equals(event.getVelocity())) {
					player.setVelocity(event.getVelocity());
				}
			}

			if (!cancelled) {
				this.broadcastIncludingSelf(new PacketPlayOutEntityVelocity(this.tracker));
			}

			this.tracker.velocityChanged = false;
		}
	}

	@Override
	public void a(final EntityPlayer entityplayer) {
		if (removeTrackedPlayer(entityplayer)) {
			this.tracker.c(entityplayer);
			entityplayer.c(this.tracker);
		}
	}

	@Override
	public void updatePlayer(EntityPlayer entityplayer) {
		AsyncCatcher.catchOp("player tracker update");
		if (entityplayer != this.tracker) {
			if (c(entityplayer)) {
				if (!trackedPlayers.contains(entityplayer) && (canPlayerSeeTrackerChunk(entityplayer) || this.tracker.attachedToPlayer)) {
					if (this.tracker instanceof EntityPlayer) {
						Player player = ((EntityPlayer) this.tracker).getBukkitEntity();
						if (!entityplayer.getBukkitEntity().canSee(player)) {
							return;
						}
					}

					entityplayer.d(this.tracker);
					addTrackedPlayer(entityplayer);
					Packet<?> spawnPacket = createSpawnPacket();
					this.lastLocationX = this.tracker.locX;
					this.lastLocationY = this.tracker.locY;
					this.lastLocationZ = this.tracker.locZ;
					this.trackerYaw = this.tracker.yaw;
					this.trackerPitch = this.tracker.pitch;
					entityplayer.playerConnection.sendPacket(spawnPacket);
					this.trackerHeadYaw = this.tracker.getHeadRotation();
					broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte) MathHelper.d((this.trackerHeadYaw * 256.0f) / 360.0f)));

					if (!this.tracker.getDataWatcher().d()) {
						entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(this.tracker.getId(), this.tracker.getDataWatcher(), true));
					}

					if (this.tracker instanceof EntityLiving) {
						EntityLiving entityliving = (EntityLiving) this.tracker;
						Collection<AttributeInstance> updateAttrs = ((AttributeMapServer) entityliving.getAttributeMap()).c();
						if (this.tracker.getId() == entityplayer.getId()) {
							((EntityPlayer) this.tracker).getBukkitEntity().injectScaledMaxHealth(updateAttrs, false);
						}
						if (!updateAttrs.isEmpty()) {
							entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateAttributes(this.tracker.getId(), updateAttrs));
						}
						for (EnumItemSlot enumitemslot : EnumItemSlot.values()) {
							ItemStack itemstack = entityliving.getEquipment(enumitemslot);
							if (!itemstack.isEmpty()) {
								entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(this.tracker.getId(), enumitemslot, itemstack));
							}
						}
						for (MobEffect mobeffect : entityliving.getEffects()) {
							entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEffect(this.tracker.getId(), mobeffect));
						}
					}

					if (updateVelocity && !(spawnPacket instanceof PacketPlayOutSpawnEntityLiving)) {
						this.lastMotionX = this.tracker.motX;
						this.lastMotionY = this.tracker.motY;
						this.lastMotionZ = this.tracker.motZ;

						entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.tracker.motX, this.tracker.motY, this.tracker.motZ));
					}

					if (this.tracker instanceof EntityHuman) {
						EntityHuman entityhuman = (EntityHuman) this.tracker;
						if (entityhuman.isSleeping()) {
							entityplayer.playerConnection.sendPacket(new PacketPlayOutBed(entityhuman, new BlockPosition(this.tracker)));
						}
					}

					if (!this.tracker.bF().isEmpty()) {
						entityplayer.playerConnection.sendPacket(new PacketPlayOutMount(this.tracker));
					}

					if (this.tracker.isPassenger()) {
						entityplayer.playerConnection.sendPacket(new PacketPlayOutMount(this.tracker.bJ()));
					}

					this.tracker.b(entityplayer);
					entityplayer.d(this.tracker);
				}
			} else {
				a(entityplayer);
			}
		}
	}

	protected void addTrackedPlayer(EntityPlayer entityplayer) {
		if (paperTrackedPlayersMapPresent) {
			trackedPlayerMap.put(entityplayer, Boolean.TRUE);
		} else {
			trackedPlayers.add(entityplayer);
		}
	}

	protected boolean removeTrackedPlayer(EntityPlayer entityplayer) {
		if (paperTrackedPlayersMapPresent) {
			return trackedPlayerMap.remove(entityplayer) != null;
		} else {
			return trackedPlayers.remove(entityplayer);
		}
	}

	@Override
	public boolean c(EntityPlayer entityplayer) {
		double diffX = entityplayer.locX - this.tracker.locX;
		double diffZ = entityplayer.locZ - this.tracker.locZ;
		int lTrackRange = Math.min(this.trackingRange, viewDistance);
		return (diffX >= -lTrackRange) && (diffX <= lTrackRange) && (diffZ >= -lTrackRange) && (diffZ <= lTrackRange) && this.tracker.a(entityplayer);
	}

	private void updateMetadataAndAttributes() {
		DataWatcher datawatcher = this.tracker.getDataWatcher();

		if (datawatcher.a()) {
			this.broadcastIncludingSelf(new PacketPlayOutEntityMetadata(this.tracker.getId(), datawatcher, false));
		}

		if (this.tracker instanceof EntityLiving) {
			AttributeMapServer attributeMapServer = (AttributeMapServer) ((EntityLiving) this.tracker).getAttributeMap();
			Set set = attributeMapServer.getAttributes();

			if (!set.isEmpty()) {
				if (this.tracker instanceof EntityPlayer) {
					((EntityPlayer) this.tracker).getBukkitEntity().injectScaledMaxHealth(set, false);
				}

				this.broadcastIncludingSelf(new PacketPlayOutUpdateAttributes(this.tracker.getId(), set));
			}

			set.clear();
		}
	}

	protected boolean canPlayerSeeTrackerChunk(EntityPlayer entityplayer) {
		return entityplayer.x().getPlayerChunkMap().a(entityplayer, this.tracker.ab, this.tracker.ad);
	}

	protected static final CachedInstanceOfChain<Function<Entity, Packet<?>>> createSpawnPacketMethods = new CachedInstanceOfChain<>();
	static {
		createSpawnPacketMethods.setKnownPath(EntityPlayer.class, entity -> new PacketPlayOutNamedEntitySpawn((EntityHuman) entity));
		createSpawnPacketMethods.setKnownPath(IAnimal.class, entity -> new PacketPlayOutSpawnEntityLiving((EntityLiving) entity));
		createSpawnPacketMethods.setKnownPath(EntityPainting.class, entity -> new PacketPlayOutSpawnEntityPainting((EntityPainting) entity));
		createSpawnPacketMethods.setKnownPath(EntityItem.class, entity -> new PacketPlayOutSpawnEntity(entity, 2, 1));
		createSpawnPacketMethods.setKnownPath(EntityMinecartAbstract.class, entity -> {
			EntityMinecartAbstract entityminecartabstract = (EntityMinecartAbstract) entity;
			return new PacketPlayOutSpawnEntity(entityminecartabstract, 10, entityminecartabstract.v().a());
		});
		createSpawnPacketMethods.setKnownPath(EntityBoat.class, entity -> new PacketPlayOutSpawnEntity(entity, 1));
		createSpawnPacketMethods.setKnownPath(EntityExperienceOrb.class, entity -> new PacketPlayOutSpawnEntityExperienceOrb((EntityExperienceOrb) entity));
		createSpawnPacketMethods.setKnownPath(EntityFishingHook.class, entity -> {
			EntityHuman entityhuman = ((EntityFishingHook) entity).l();
			return new PacketPlayOutSpawnEntity(entity, 90, (entityhuman == null) ? entity.getId() : entityhuman.getId());
		});
		createSpawnPacketMethods.setKnownPath(EntitySpectralArrow.class, entity -> {
			Entity shooter = ((EntitySpectralArrow) entity).shooter;
			return new PacketPlayOutSpawnEntity(entity, 91, 1 + ((shooter == null) ? entity.getId() : shooter.getId()));
		});
		createSpawnPacketMethods.setKnownPath(EntityTippedArrow.class, entity -> {
			Entity shooter = ((EntityArrow) entity).shooter;
			return new PacketPlayOutSpawnEntity(entity, 60, 1 + ((shooter == null) ? entity.getId() : shooter.getId()));
		});
		createSpawnPacketMethods.setKnownPath(EntitySnowball.class, entity -> new PacketPlayOutSpawnEntity(entity, 61));
		createSpawnPacketMethods.setKnownPath(EntityLlamaSpit.class, entity -> new PacketPlayOutSpawnEntity(entity, 68));
		createSpawnPacketMethods.setKnownPath(EntityPotion.class, entity -> new PacketPlayOutSpawnEntity(entity, 73));
		createSpawnPacketMethods.setKnownPath(EntityThrownExpBottle.class, entity -> new PacketPlayOutSpawnEntity(entity, 75));
		createSpawnPacketMethods.setKnownPath(EntityEnderPearl.class, entity -> new PacketPlayOutSpawnEntity(entity, 65));
		createSpawnPacketMethods.setKnownPath(EntityEnderSignal.class, entity -> new PacketPlayOutSpawnEntity(entity, 72));
		createSpawnPacketMethods.setKnownPath(EntityFireworks.class, entity -> new PacketPlayOutSpawnEntity(entity, 76));
		createSpawnPacketMethods.setKnownPath(EntityFireball.class, entity -> {
			EntityFireball entityfireball = (EntityFireball) entity;
			byte objectTypeId = 63;
			if (entityfireball instanceof EntitySmallFireball) {
				objectTypeId = 64;
			} else if (entityfireball instanceof EntityDragonFireball) {
				objectTypeId = 93;
			} else if (entityfireball instanceof EntityWitherSkull) {
				objectTypeId = 66;
			}
			PacketPlayOutSpawnEntity packet = null;
			if (entityfireball.shooter != null) {
				packet = new PacketPlayOutSpawnEntity(entityfireball, objectTypeId, entityfireball.shooter.getId());
			} else {
				packet = new PacketPlayOutSpawnEntity(entityfireball, objectTypeId, 0);
			}
			packet.a((int) (entityfireball.dirX * 8000.0));
			packet.b((int) (entityfireball.dirY * 8000.0));
			packet.c((int) (entityfireball.dirZ * 8000.0));
			return packet;
		});
		createSpawnPacketMethods.setKnownPath(EntityShulkerBullet.class, entity -> {
			PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(entity, 67, 0);
			packet.a((int) (entity.motX * 8000.0));
			packet.b((int) (entity.motY * 8000.0));
			packet.c((int) (entity.motZ * 8000.0));
			return packet;
		});
		createSpawnPacketMethods.setKnownPath(EntityEgg.class, entity -> new PacketPlayOutSpawnEntity(entity, 62));
		createSpawnPacketMethods.setKnownPath(EntityEvokerFangs.class, entity -> new PacketPlayOutSpawnEntity(entity, 79));
		createSpawnPacketMethods.setKnownPath(EntityTNTPrimed.class, entity -> new PacketPlayOutSpawnEntity(entity, 50));
		createSpawnPacketMethods.setKnownPath(EntityEnderCrystal.class, entity -> new PacketPlayOutSpawnEntity(entity, 51));
		createSpawnPacketMethods.setKnownPath(EntityFallingBlock.class, entity -> {
			EntityFallingBlock entityfallingblock = (EntityFallingBlock) entity;
			return new PacketPlayOutSpawnEntity(entity, 70, Block.getCombinedId(entityfallingblock.getBlock()));
		});
		createSpawnPacketMethods.setKnownPath(EntityArmorStand.class, entity -> new PacketPlayOutSpawnEntity(entity, 78));
		createSpawnPacketMethods.setKnownPath(EntityItemFrame.class, entity -> {
			EntityItemFrame entityitemframe = (EntityItemFrame) entity;
			return new PacketPlayOutSpawnEntity(entity, 71, entityitemframe.direction.get2DRotationValue(), entityitemframe.getBlockPosition());
		});
		createSpawnPacketMethods.setKnownPath(EntityLeash.class, entity -> {
			EntityLeash entityleash = (EntityLeash) entity;
			return new PacketPlayOutSpawnEntity(entity, 77, 0, entityleash.getBlockPosition());
		});
		createSpawnPacketMethods.setKnownPath(EntityAreaEffectCloud.class, entity -> new PacketPlayOutSpawnEntity(entity, 3));
		EntityTypes.b.iterator().forEachRemaining(createSpawnPacketMethods::selectPath);
	}

	protected Packet<?> createSpawnPacket() {
		if (this.tracker.dead) {
			return null;
		}
		Function<Entity, Packet<?>> createSpawnPacketMethod = createSpawnPacketMethods.selectPath(this.tracker.getClass());
		if (createSpawnPacketMethod == null) {
			throw new IllegalArgumentException("Don't know how to add " + this.tracker.getClass() + "!");
		}
		return createSpawnPacketMethod.apply(this.tracker);
	}

	@Override
	public void a(int i) {
		this.viewDistance = i;
	}

	@Override
	public void c() {
	}

}
