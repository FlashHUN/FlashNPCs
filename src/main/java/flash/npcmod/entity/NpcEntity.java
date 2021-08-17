package flash.npcmod.entity;

import flash.npcmod.init.EntityInit;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestDialogue;
import flash.npcmod.network.packets.client.CRequestDialogueEditor;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class NpcEntity extends AmbientEntity {

  private static final DataParameter<String> DIALOGUE = EntityDataManager.createKey(NpcEntity.class, DataSerializers.STRING);
  private static final DataParameter<Integer> TEXTCOLOR = EntityDataManager.createKey(NpcEntity.class, DataSerializers.VARINT);
  private static final DataParameter<String> TEXTURE = EntityDataManager.createKey(NpcEntity.class, DataSerializers.STRING);
  private static final DataParameter<Boolean> SLIM = EntityDataManager.createKey(NpcEntity.class, DataSerializers.BOOLEAN);
  private static final DataParameter<BlockPos> ORIGIN = EntityDataManager.createKey(NpcEntity.class, DataSerializers.BLOCK_POS);

  private int teleportCounter;
  private static final int MAX_TELEPORT_COUNTER = 20*20; // 20 ticks (1 second) * amount of seconds

  public NpcEntity(EntityType<? extends AmbientEntity> type, World world) {
    super(type, world);
    teleportCounter = 0;
  }

  private NpcEntity(World world) {
    this(EntityInit.NPC_ENTITY.get(), world);
  }

  @Override
  protected void registerGoals() {
    super.registerGoals();
    this.goalSelector.addGoal(10, new LookAtGoal(this, PlayerEntity.class, 8.0F));
  }

  @Override
  protected void registerData() {
    super.registerData();
    this.dataManager.register(DIALOGUE, "");
    this.dataManager.register(TEXTCOLOR, 0xFFFFFF);
    this.dataManager.register(TEXTURE, "");
    this.dataManager.register(SLIM, Boolean.FALSE);
    this.dataManager.register(ORIGIN, BlockPos.ZERO);
  }

  @Override
  public void writeAdditional(CompoundNBT compound) {
    super.writeAdditional(compound);
    compound.putString("dialogue", getDialogue());
    compound.putInt("textColor", getTextColor());
    compound.putString("texture", getTexture());
    compound.putBoolean("slim", isSlim());
    compound.put("origin", NBTUtil.writeBlockPos(getOrigin()));
  }

  @Override
  public void readAdditional(CompoundNBT compound) {
    super.readAdditional(compound);
    setDialogue(compound.getString("dialogue"));
    setTextColor(compound.getInt("textColor"));
    setTexture(compound.getString("texture"));
    setSlim(compound.getBoolean("slim"));
    setOrigin(NBTUtil.readBlockPos(compound.getCompound("origin")));
  }

  public String getDialogue() {
    return this.dataManager.get(DIALOGUE);
  }

  public void setDialogue(String s) {
    this.dataManager.set(DIALOGUE, s);
  }

  public int getTextColor() {
    return this.dataManager.get(TEXTCOLOR);
  }

  public void setTextColor(int i) {
    this.dataManager.set(TEXTCOLOR, i);
  }

  public String getTexture() {
    return this.dataManager.get(TEXTURE);
  }

  public void setTexture(String s) {
    this.dataManager.set(TEXTURE, s);
  }

  public boolean isSlim() {
    return this.dataManager.get(SLIM);
  }

  public void setSlim(boolean b) {
    this.dataManager.set(SLIM, b);
  }

  public BlockPos getOrigin() {
    return this.dataManager.get(ORIGIN);
  }

  public void setOrigin(BlockPos pos) {
    this.dataManager.set(ORIGIN, pos);
  }

  protected SoundEvent getSwimSound() {
    return SoundEvents.ENTITY_PLAYER_SWIM;
  }

  protected SoundEvent getSplashSound() {
    return SoundEvents.ENTITY_PLAYER_SPLASH;
  }

  protected SoundEvent getHighspeedSplashSound() {
    return SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED;
  }

  protected SoundEvent getFallSound(int heightIn) {
    return heightIn > 4 ? SoundEvents.ENTITY_PLAYER_BIG_FALL : SoundEvents.ENTITY_PLAYER_SMALL_FALL;
  }

  protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
    if (damageSourceIn == DamageSource.ON_FIRE) {
      return SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE;
    } else if (damageSourceIn == DamageSource.DROWN) {
      return SoundEvents.ENTITY_PLAYER_HURT_DROWN;
    } else {
      return damageSourceIn == DamageSource.SWEET_BERRY_BUSH ? SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH : SoundEvents.ENTITY_PLAYER_HURT;
    }
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.ENTITY_PLAYER_DEATH;
  }

  public SoundCategory getSoundCategory() {
    return SoundCategory.PLAYERS;
  }

  @Override
  protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
    return 1.8f * 0.85F;
  }

  public static AttributeModifierMap.MutableAttribute setCustomAttributes() {
    return MobEntity.func_233666_p_()
        .createMutableAttribute(Attributes.MAX_HEALTH, 20.0D)
        .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.2D);
  }

  @Override
  public boolean hitByEntity(Entity entityIn) {
    if (entityIn instanceof PlayerEntity) {
      PlayerEntity player = (PlayerEntity)entityIn;
      if (player.hasPermissionLevel(4) && player.isCreative() && player.getHeldItem(Hand.MAIN_HAND).getItem() instanceof NpcEditorItem) {
        this.onKillCommand();
      }
    }
    return true;
  }

  @Override
  public ActionResultType applyPlayerInteraction(PlayerEntity player, Vector3d vec, Hand hand) {
    if (hand.equals(Hand.MAIN_HAND)) {
      String name = getDialogue();
      // If we have a dialogue bound to the npc
      if (!name.isEmpty()) {
        if (!(player.getHeldItem(hand).getItem() instanceof NpcEditorItem)) {
          // If the player doesn't have an NpcEditorItem in their hand, send them the dialogue and open the screen for it
          if (player.world.isRemote) {
            PacketDispatcher.sendToServer(new CRequestDialogue(name, this.getEntityId()));
          }
        } else {
          // Otherwise if they're opped, in creative mode, and sneaking, send them the dialogue editor and open the screen for it
          if (player.hasPermissionLevel(4) && player.isCreative() && player.isSneaking()) {
            if (player.world.isRemote) {
              PacketDispatcher.sendToServer(new CRequestDialogue(name, this.getEntityId()));
              PacketDispatcher.sendToServer(new CRequestDialogueEditor(name, this.getEntityId()));
            }
          }
        }
      }
    }
    return ActionResultType.PASS;
  }

  @Nullable
  @Override
  public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
    this.setTextColor(0xFFFFFF);
    return super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
  }

  @Override
  public void tick() {
    super.tick();
    if (isAlive()) {
      if (!world.isRemote) {
        if (this.getOrigin().equals(BlockPos.ZERO)) {
          this.setOrigin(this.getPosition());
        }
        teleportCounter++;
        if (teleportCounter > MAX_TELEPORT_COUNTER) {
          this.teleportCounter = 0;
          BlockPos origin = getOrigin();
          if (!this.getPosition().equals(origin)) {
            this.setPosition(origin.getX()+0.5, origin.getY()+0.5, origin.getZ()+0.5);
          }
        }
      }
    }
  }

  @Override
  public boolean canDespawn(double distanceToClosestPlayer) {
    return false;
  }
}
