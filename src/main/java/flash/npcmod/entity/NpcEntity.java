package flash.npcmod.entity;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.ItemUtil;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjectiveTypes;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.init.EntityInit;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.item.NpcSaveToolItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestDialogue;
import flash.npcmod.network.packets.client.CRequestDialogueEditor;
import flash.npcmod.network.packets.client.CRequestTrades;
import flash.npcmod.network.packets.server.SCompleteQuest;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;

public class NpcEntity extends AmbientCreature {

  private static final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.3F);
  private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, Player.STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, SITTING_DIMENSIONS).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
  private static final EntityDataAccessor<String> DIALOGUE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Integer> TEXTCOLOR = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<BlockPos> ORIGIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BLOCK_POS);
  private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> CROUCHING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);

  @Nullable
  private TradeOffers tradeOffers;
  public static final int MAX_OFFERS = 12;

  private int teleportCounter;
  private static final int MAX_TELEPORT_COUNTER = 20*15; // 20 ticks (1 second) * amount of seconds

  public NpcEntity(EntityType<? extends AmbientCreature> type, Level world) {
    super(type, world);
    teleportCounter = 0;
    this.setPersistenceRequired();
  }

  protected NpcEntity(Level world) {
    this(EntityInit.NPC_ENTITY.get(), world);
  }

  @Override
  protected boolean canRide(Entity entity) {
    return false; // TODO default riding mobs like horses in the future?
  }

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    return POSES.getOrDefault(pose, Player.STANDING_DIMENSIONS);
  }

  @Override
  protected void registerGoals() {
    super.registerGoals();
    this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(DIALOGUE, "");
    this.entityData.define(TEXTCOLOR, 0xFFFFFF);
    this.entityData.define(TEXTURE, "");
    this.entityData.define(SLIM, false);
    this.entityData.define(ORIGIN, BlockPos.ZERO);
    this.entityData.define(SITTING, false);
    this.entityData.define(CROUCHING, false);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    compound.putString("dialogue", getDialogue());
    compound.putBoolean("nameVisibility", isCustomNameVisible());
    compound.putInt("textColor", getTextColor());
    compound.putString("texture", getTexture());
    compound.putBoolean("slim", isSlim());
    compound.put("origin", NbtUtils.writeBlockPos(getOrigin()));
    compound.putBoolean("sitting", isSitting());
    compound.putBoolean("crouching", isCrouching());

    TradeOffers tradeOffers = this.getOffers();
    if (!tradeOffers.isEmpty()) {
      compound.put("Offers", tradeOffers.write());
    }
  }

  @Override
  public void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    setDialogue(compound.getString("dialogue"));
    setCustomNameVisible(compound.getBoolean("nameVisibility"));
    setTextColor(compound.getInt("textColor"));
    setTexture(compound.getString("texture"));
    setSlim(compound.getBoolean("slim"));
    setOrigin(NbtUtils.readBlockPos(compound.getCompound("origin")));
    setSitting(compound.getBoolean("sitting"));
    setCrouching(compound.getBoolean("crouching"));

    if (compound.contains("Offers", 10)) {
      this.tradeOffers = new TradeOffers(compound.getCompound("Offers"));
    }
  }

  public TradeOffers getOffers() {
    if (this.tradeOffers == null) {
      this.tradeOffers = new TradeOffers();
    }
    if (this.tradeOffers.size() < MAX_OFFERS) {
      for (int i = this.tradeOffers.size(); i < MAX_OFFERS; i++) {
        this.tradeOffers.add(new TradeOffer());
      }
    }

    return this.tradeOffers;
  }

  @OnlyIn(Dist.CLIENT)
  public void setTradeOffers(TradeOffers tradeOffers) {
    this.tradeOffers = tradeOffers;
  }

  public String getDialogue() {
    return this.entityData.get(DIALOGUE);
  }

  public void setDialogue(String s) {
    this.entityData.set(DIALOGUE, s);
  }

  public int getTextColor() {
    return this.entityData.get(TEXTCOLOR);
  }

  public void setTextColor(int i) {
    this.entityData.set(TEXTCOLOR, i);
  }

  public String getTexture() {
    return this.entityData.get(TEXTURE);
  }

  public void setTexture(String s) {
    this.entityData.set(TEXTURE, s);
  }

  public boolean isSlim() {
    return this.entityData.get(SLIM);
  }

  public void setSlim(boolean b) {
    this.entityData.set(SLIM, b);
  }

  public BlockPos getOrigin() {
    return this.entityData.get(ORIGIN);
  }

  public void setOrigin(BlockPos pos) {
    this.entityData.set(ORIGIN, pos);
  }

  public boolean isSitting() {
    return this.entityData.get(SITTING);
  }

  public void setSitting(boolean b) {
    this.entityData.set(SITTING, b);
    if (b) {
      this.setPose(Pose.SPIN_ATTACK);
    }
    else {
      this.setPose(isCrouching() ? Pose.CROUCHING : Pose.STANDING);
    }
  }

  public boolean isCrouching() {
    return this.entityData.get(CROUCHING);
  }

  public void setCrouching(boolean b) {
    this.entityData.set(CROUCHING, b);
    if (b) {
      this.setPose(Pose.CROUCHING);
    }
    else {
      this.setPose(isSitting() ? Pose.SPIN_ATTACK : Pose.STANDING);
    }
  }

  protected SoundEvent getSwimSound() {
    return SoundEvents.PLAYER_SWIM;
  }

  protected SoundEvent getSwimSplashSound() {
    return SoundEvents.PLAYER_SPLASH;
  }

  protected SoundEvent getSwimHighSpeedSplashSound() {
    return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
  }

  protected SoundEvent getFallDamageSound(int heightIn) {
    return heightIn > 4 ? SoundEvents.PLAYER_BIG_FALL : SoundEvents.PLAYER_SMALL_FALL;
  }

  protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
    if (damageSourceIn == DamageSource.ON_FIRE) {
      return SoundEvents.PLAYER_HURT_ON_FIRE;
    } else if (damageSourceIn == DamageSource.DROWN) {
      return SoundEvents.PLAYER_HURT_DROWN;
    } else {
      return damageSourceIn == DamageSource.SWEET_BERRY_BUSH ? SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH : SoundEvents.PLAYER_HURT;
    }
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.PLAYER_DEATH;
  }

  public SoundSource getSoundSource() {
    return SoundSource.PLAYERS;
  }

  @Override
  protected float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
    return getDimensions(poseIn).height * 0.85F;
  }

  public static AttributeSupplier.Builder setCustomAttributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.2D);
  }

  @Override
  public boolean skipAttackInteraction(Entity entityIn) {
    if (entityIn instanceof Player) {
      Player player = (Player)entityIn;
      if (player.hasPermissions(4) && player.isCreative() && player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof NpcEditorItem) {
        this.kill();
      }
    }
    return true;
  }

  @Override
  public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
    if (hand.equals(InteractionHand.MAIN_HAND)) {
      if (player != null && player.isAlive()) {
        IQuestCapability questCapability = QuestCapabilityProvider.getCapability(player);

        List<QuestInstance> markedForCompletion = new ArrayList<>();

        for (QuestInstance questInstance : questCapability.getAcceptedQuests()) {
          if (questInstance.getPickedUpFrom().equals(this.getUUID()) && questInstance.getQuest().canComplete()) {
            markedForCompletion.add(questInstance);
          }
        }

        for (QuestInstance questInstance : markedForCompletion) {
          questCapability.completeQuest(questInstance);
          if (!level.isClientSide) {
            PacketDispatcher.sendTo(new SCompleteQuest(questInstance.getQuest().getName(), questInstance.getPickedUpFrom()), player);
          }
        }

        if (markedForCompletion.isEmpty()) {

          String name = getDialogue();
          // If we have a dialogue bound to the npc
          if (!name.isEmpty()) {
            if (!(player.getItemInHand(hand).getItem() instanceof NpcEditorItem || player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem)) {
              // If the player doesn't have an NpcEditorItem in their hand, send them the dialogue and open the screen for it
              if (player.level.isClientSide) {
                PacketDispatcher.sendToServer(new CRequestDialogue(name, this.getId()));
              }
            } else if (!(player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem)) {
              // Otherwise if they're opped, in creative mode, and sneaking, send them the dialogue editor and open the screen for it
              if (player.hasPermissions(4) && player.isCreative() && player.isShiftKeyDown()) {
                if (player.level.isClientSide) {
                  PacketDispatcher.sendToServer(new CRequestDialogue(name, this.getId()));
                  PacketDispatcher.sendToServer(new CRequestDialogueEditor(name, this.getId()));
                }
              }
            }
          } else {
            // If the NPC has trades, they don't have any dialogue, and we don't have the requirements to edit the npc in any way,
            // open the trades gui for this npc
            if (!((player.getItemInHand(hand).getItem() instanceof NpcEditorItem || player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem) && player.hasPermissions(4) && player.isCreative())) {
              if (player.level.isClientSide) {
                PacketDispatcher.sendToServer(new CRequestTrades(this.getId()));
              }
            }
          }
        }
      }
    }
    return InteractionResult.PASS;
  }

  @Nullable
  @Override
  public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
    this.setTextColor(0xFFFFFF);
    return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
  }

  @Override
  public void tick() {
    super.tick();
    if (isAlive()) {
      if (!level.isClientSide) {
        if (this.getOrigin().equals(BlockPos.ZERO)) {
          this.setOrigin(this.blockPosition());
        }
        teleportCounter++;
        if (teleportCounter > MAX_TELEPORT_COUNTER) {
          this.teleportCounter = 0;
          BlockPos origin = getOrigin();
          if (!this.blockPosition().equals(origin)) {
            this.setPos(origin.getX()+0.5, origin.getY()+0.5, origin.getZ()+0.5);
          }
        }
      }
    }
  }

  @Override
  public boolean removeWhenFarAway(double distanceToClosestPlayer) {
    return false;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("name", getName().getString());
    jsonObject.addProperty("nameVisibility", isCustomNameVisible());
    jsonObject.addProperty("dialogue", getDialogue());
    jsonObject.addProperty("texture", getTexture());
    jsonObject.addProperty("textColor", getTextColor());
    jsonObject.addProperty("slim", isSlim());
    jsonObject.addProperty("pose", isSitting() ? "sitting" : isCrouching() ? "crouching" : "standing");
    jsonObject.addProperty("trades", getOffers().write().getAsString());

    jsonObject.add("inventory", inventoryToJson());

    return jsonObject;
  }

  private JsonObject inventoryToJson() {
    JsonObject inventory = new JsonObject();
    inventory.addProperty("mainHand", ItemUtil.stackToString(getMainHandItem()));
    inventory.addProperty("offHand", ItemUtil.stackToString(getOffhandItem()));
    inventory.addProperty("head", ItemUtil.stackToString(getItemBySlot(EquipmentSlot.HEAD)));
    inventory.addProperty("chest", ItemUtil.stackToString(getItemBySlot(EquipmentSlot.CHEST)));
    inventory.addProperty("legs", ItemUtil.stackToString(getItemBySlot(EquipmentSlot.LEGS)));
    inventory.addProperty("feet", ItemUtil.stackToString(getItemBySlot(EquipmentSlot.FEET)));
    return inventory;
  }

  public static NpcEntity fromJson(Level level, JsonObject jsonObject) {
    NpcEntity npcEntity = EntityInit.NPC_ENTITY.get().create(level);
    npcEntity.setCustomName(new TextComponent(jsonObject.get("name").getAsString()));
    npcEntity.setCustomNameVisible(jsonObject.get("nameVisibility").getAsBoolean());
    npcEntity.setDialogue(jsonObject.get("dialogue").getAsString());
    npcEntity.setTexture(jsonObject.get("texture").getAsString());
    npcEntity.setTextColor(jsonObject.get("textColor").getAsInt());
    npcEntity.setSlim(jsonObject.get("slim").getAsBoolean());
    String pose = jsonObject.get("pose").getAsString();
    switch (pose) {
      case "sitting" -> npcEntity.setSitting(true);
      case "crouching" -> npcEntity.setCrouching(true);
    }
    npcEntity.tradeOffers = TradeOffers.read(jsonObject.get("trades").getAsString());

    JsonObject inventory = jsonObject.getAsJsonObject("inventory");
    npcEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemUtil.stackFromString(inventory.get("mainHand").getAsString()));
    npcEntity.setItemSlot(EquipmentSlot.OFFHAND, ItemUtil.stackFromString(inventory.get("offHand").getAsString()));
    npcEntity.setItemSlot(EquipmentSlot.HEAD, ItemUtil.stackFromString(inventory.get("head").getAsString()));
    npcEntity.setItemSlot(EquipmentSlot.CHEST, ItemUtil.stackFromString(inventory.get("chest").getAsString()));
    npcEntity.setItemSlot(EquipmentSlot.LEGS, ItemUtil.stackFromString(inventory.get("legs").getAsString()));
    npcEntity.setItemSlot(EquipmentSlot.FEET, ItemUtil.stackFromString(inventory.get("feet").getAsString()));

    return npcEntity;
  }
}
