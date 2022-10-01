package flash.npcmod.entity;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.behaviors.Action;
import flash.npcmod.core.behaviors.Behavior;
import flash.npcmod.core.behaviors.Trigger;
import flash.npcmod.core.ItemUtil;
import flash.npcmod.core.behaviors.BehaviorSavedData;
import flash.npcmod.core.client.behaviors.ClientBehaviorUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.goals.NPCFollowPathGoal;
import flash.npcmod.entity.goals.NPCInteractWithBlockGoal;
import flash.npcmod.entity.goals.NPCWanderGoal;
import flash.npcmod.entity.goals.TalkWithPlayerGoal;
import flash.npcmod.init.EntityInit;
import flash.npcmod.item.BehaviorEditorItem;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.item.NpcSaveToolItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.*;
import flash.npcmod.network.packets.server.SCompleteQuest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class NpcEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> TITLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> RENDERER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> RENDERER_TAG = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> BEHAVIOR_FILE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> CROUCHING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Behavior> CURRENT_BEHAVIOR = SynchedEntityData.defineId(NpcEntity.class, NpcDataSerializers.BEHAVIOR);
    private static final EntityDataAccessor<String> DIALOGUE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> GOAL_REACHED = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<BlockPos> ORIGIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.3F);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> TARGET_BLOCK = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> TRIGGER_TIMER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TEXTCOLOR = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_TEXTURE_RESOURCE_LOCATION = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> SCALE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, Player.STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, SITTING_DIMENSIONS).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();

    public static final int MAX_OFFERS = 12;
    private static final int MAX_TELEPORT_COUNTER = 20 * 15; // 20 ticks (1 second) * amount of seconds
    @Nullable
    private Player talkingPlayer;
    private int teleportCounter;

    private int triggerTickCounter;

    @Nullable
    private TradeOffers tradeOffers;
    private LivingEntity entityToRenderAs;
    private float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    private CompoundTag previousRendererTag;
    private Component titleComponent = TextComponent.EMPTY;

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        teleportCounter = 0;
        triggerTickCounter = 0;
        this.setPersistenceRequired();
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
    }

    protected NpcEntity(Level world) {
        this(EntityInit.NPC_ENTITY.get(), world);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
    }

    /**
     * Add additional save data to the nbt compound tag.
     * @param compound The original compound tag.
     */
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("title", getTitle());
        compound.putString("dialogue", getDialogue());
        compound.putString("behaviorFile", getBehaviorFile());
        compound.put("currentBehavior", getCurrentBehavior().toCompoundTag());
        compound.putBoolean("nameVisibility", isCustomNameVisible());
        compound.putInt("radius", getRadius());
        compound.putBoolean("slim", isSlim());
        compound.put("targetBlock", NbtUtils.writeBlockPos(getTargetBlock()));
        compound.putInt("textColor", getTextColor());
        compound.putInt("triggerTimer", getTriggerTimer());
        compound.putString("texture", getTexture());
        compound.putBoolean("is_texture_resource_loc", isTextureResourceLocation());
        compound.put("origin", NbtUtils.writeBlockPos(getOrigin()));
        compound.putBoolean("sitting", isSitting());
        compound.putBoolean("crouching", isCrouching());
        compound.putString("renderer", getRenderer());
        if (getRendererTag() != null)
            compound.put("rendererTag", getRendererTag());
        compound.putFloat("scaleX", getScaleX());
        compound.putFloat("scaleY", getScaleY());
        compound.putFloat("scaleZ", getScaleZ());

        TradeOffers tradeOffers = this.getOffers();
        if (!tradeOffers.isEmpty()) {
            compound.put("Offers", tradeOffers.write());
        }
    }

    /**
     * Read additional save data.
     * @param compound The compound tag.
     */
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("behaviorFile")) setBehaviorFile(compound.getString("behaviorFile"));
        setCrouching(compound.getBoolean("crouching"));
        if (compound.contains("currentBehavior")) setCurrentBehavior(
                Behavior.fromCompoundTag(compound.getCompound("currentBehavior")));
        setCustomNameVisible(compound.getBoolean("nameVisibility"));
        setTitle(compound.getString("title"));
        setDialogue(compound.getString("dialogue"));
        setOrigin(NbtUtils.readBlockPos(compound.getCompound("origin")));
        setRadius(compound.getInt("radius"));
        setSitting(compound.getBoolean("sitting"));
        setSlim(compound.getBoolean("slim"));
        setTargetBlock(NbtUtils.readBlockPos(compound.getCompound("targetBlock")));
        setTextColor(compound.getInt("textColor"));
        setTexture(compound.getString("texture"));
        if (compound.contains("is_texture_resource_loc"))
            setIsTextureResourceLocation(compound.getBoolean("is_texture_resource_loc"));
        setRenderer(compound.getString("renderer"));
        if (compound.contains("rendererTag"))
            setRendererTag((CompoundTag) compound.get("rendererTag"));
        setScale(compound.getFloat("scaleX"), compound.getFloat("scaleY"), compound.getFloat("scaleZ"));

        if (compound.contains("Offers", 10)) {
            this.tradeOffers = new TradeOffers(compound.getCompound("Offers"));
        }
    }

    /**
     * Convert this npc to JSON.
     * @return JsonObject.
     */
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", getName().getString());
        jsonObject.addProperty("nameVisibility", isCustomNameVisible());
        jsonObject.addProperty("dialogue", getDialogue());
        jsonObject.addProperty("title", getTitle());

        jsonObject.add("currentBehavior", getCurrentBehavior().toJSON());
        jsonObject.addProperty("targetBlock", getTargetBlock().asLong());
        jsonObject.addProperty("texture", getTexture());
        jsonObject.addProperty("is_texture_resource_loc", isTextureResourceLocation());
        jsonObject.addProperty("textColor", getTextColor());
        jsonObject.addProperty("slim", isSlim());
        jsonObject.addProperty("pose", isSitting() ? "sitting" : isCrouching() ? "crouching" : "standing");
        jsonObject.addProperty("trades", getOffers().write().getAsString());
        jsonObject.addProperty("renderer", getRenderer());
        if (getRendererTag() != null)
            jsonObject.addProperty("rendererTag", getRendererTag().getAsString());
        JsonObject scale = new JsonObject();
        scale.addProperty("x", scaleX);
        scale.addProperty("y", scaleY);
        scale.addProperty("z", scaleZ);
        jsonObject.add("scale", scale);

        jsonObject.add("inventory", inventoryToJson());

        return jsonObject;
    }

    /**
     * Load this entity from a json.
     * @param level The world.
     * @param jsonObject The json object.
     * @return The new npc.
     */
    public static NpcEntity fromJson(Level level, JsonObject jsonObject) {
        NpcEntity npcEntity = EntityInit.NPC_ENTITY.get().create(level);
        assert npcEntity != null;
        if (jsonObject.has("currentBehavior"))
            npcEntity.setCurrentBehavior(Behavior.fromJSONObject(jsonObject.getAsJsonObject("currentBehavior")));
        npcEntity.setCustomNameVisible(jsonObject.get("nameVisibility").getAsBoolean());
        npcEntity.setDialogue(jsonObject.get("dialogue").getAsString());
        npcEntity.setCustomName(new TextComponent(jsonObject.get("name").getAsString()));
        if (jsonObject.has("title"))
            npcEntity.setTitle(jsonObject.get("title").getAsString());
        if (jsonObject.has("radius"))
            npcEntity.setRadius(jsonObject.get("radius").getAsInt());
        npcEntity.setSlim(jsonObject.get("slim").getAsBoolean());
        if (jsonObject.has("targetBlock"))
            npcEntity.setTargetBlock(BlockPos.of(jsonObject.get("targetBlock").getAsLong()));
        npcEntity.setTexture(jsonObject.get("texture").getAsString());
        if (jsonObject.has("is_texture_resource_loc"))
            npcEntity.setIsTextureResourceLocation(jsonObject.get("is_texture_resource_loc").getAsBoolean());
        npcEntity.setTextColor(jsonObject.get("textColor").getAsInt());
        if (jsonObject.has("triggerTimer"))
            npcEntity.setTriggerTimer(jsonObject.get("triggerTimer").getAsInt());
        String pose = jsonObject.get("pose").getAsString();
        switch (pose) {
            case "sitting" -> npcEntity.setSitting(true);
            case "crouching" -> npcEntity.setCrouching(true);
        }
        if (jsonObject.has("renderer"))
            npcEntity.setRenderer(jsonObject.get("renderer").getAsString());
        if (jsonObject.has("scale")) {
            JsonObject scale = jsonObject.get("scale").getAsJsonObject();
            npcEntity.setScale(scale.get("x").getAsFloat(), scale.get("y").getAsFloat(), scale.get("z").getAsFloat());
        }
        npcEntity.tradeOffers = TradeOffers.read(jsonObject.get("trades").getAsString());
        try {
            npcEntity.setRendererTag(new TagParser(new StringReader(jsonObject.get("rendererTag").getAsString())).readStruct());
        }
        catch (Exception ignored) {}

        JsonObject inventory = jsonObject.getAsJsonObject("inventory");
        npcEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemUtil.stackFromString(inventory.get("mainHand").getAsString()));
        npcEntity.setItemSlot(EquipmentSlot.OFFHAND, ItemUtil.stackFromString(inventory.get("offHand").getAsString()));
        npcEntity.setItemSlot(EquipmentSlot.HEAD, ItemUtil.stackFromString(inventory.get("head").getAsString()));
        npcEntity.setItemSlot(EquipmentSlot.CHEST, ItemUtil.stackFromString(inventory.get("chest").getAsString()));
        npcEntity.setItemSlot(EquipmentSlot.LEGS, ItemUtil.stackFromString(inventory.get("legs").getAsString()));
        npcEntity.setItemSlot(EquipmentSlot.FEET, ItemUtil.stackFromString(inventory.get("feet").getAsString()));

        return npcEntity;
    }

    /**
     * Apply the behavior to the npc.
     * @param behavior The behavior to apply.
     */
    private void applyBehavior(Behavior behavior) {
        this.setDialogue(behavior.dialogueName);
        Action action = behavior.getAction();
        CEditNpc.NPCPose pose = action.getPose();
        switch (pose) {
            case STANDING -> {
                setSitting(false);
                setCrouching(false);
            }
            case CROUCHING -> {
                setCrouching(true);
                setSitting(false);
            }
            case SITTING -> {
                setSitting(true);
                setCrouching(false);
            }
        }
        this.setRadius(action.getRadius());
        this.setTargetBlock(action.getTargetBlockPos());
        this.setGoalReached(false);
        for (Trigger trigger : behavior.getTriggers()) {
            if (trigger.getType() == Trigger.TriggerType.TIMER_TRIGGER) {
                this.setTriggerTimer(trigger.getTimer());
            }
        }

        for (WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
            if (!(goal.getGoal() instanceof LookAtPlayerGoal))
                this.goalSelector.removeGoal(goal);
        }
        switch (action.getActionType()) {
            case FOLLOW_PATH -> this.goalSelector.addGoal(1, new NPCFollowPathGoal(this, 1.2D));
            case WANDER -> this.goalSelector.addGoal(0, new NPCWanderGoal(this, 300));
            case INTERACT_WITH -> this.goalSelector.addGoal(2, new NPCInteractWithBlockGoal(this, 1.00));
            case STANDSTILL -> {
                if (!action.getTargetBlockPos().equals(BlockPos.ZERO)) {
                    BlockPos pos = action.getTargetBlockPos();
                    this.setOrigin(pos);
                    this.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                }
            }
        }

        this.setCurrentBehavior(behavior);
    }

    /**
     * Check if this entity can ride an entity or check if it can ride
     * @param entity I'm not sure yet.
     * @return False because this entity can't ride/ be ridden.
     */
    @Override
    protected boolean canRide(@NotNull Entity entity) {
        return false; // TODO default riding mobs like horses in the future?
    }

    /**
     * Cannot be leashed.
     * @param player Player id
     * @return False.
     */
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    /**
     * Define synced data. Must be done for all EntityDataAccessor variables.
     */
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEHAVIOR_FILE, "");
        this.entityData.define(CROUCHING, false);
        this.entityData.define(CURRENT_BEHAVIOR, new Behavior());
        this.entityData.define(DIALOGUE, "");
        this.entityData.define(GOAL_REACHED, false);
        this.entityData.define(ORIGIN, BlockPos.ZERO);
        this.entityData.define(RADIUS, 0);
        this.entityData.define(TARGET_BLOCK, BlockPos.ZERO);
        this.entityData.define(TRIGGER_TIMER, 0);
        this.entityData.define(TEXTCOLOR, 0xFFFFFF);
        this.entityData.define(TEXTURE, "");
        this.entityData.define(IS_TEXTURE_RESOURCE_LOCATION, false);
        this.entityData.define(SLIM, false);
        this.entityData.define(SITTING, false);
        this.entityData.define(RENDERER, this.getType().getRegistryName().toString());
        this.entityData.define(RENDERER_TAG, new CompoundTag());
        this.entityData.define(SCALE, getDefaultScale());
        this.entityData.define(TITLE, "");
    }

    private CompoundTag getDefaultScale() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("x", 1f);
        tag.putFloat("y", 1f);
        tag.putFloat("z", 1f);
        return tag;
    }

    /**
     * Add final spawn data.
     * @param worldIn The dimension.
     * @param difficultyIn The difficulty modifier.
     * @param reason The spawn type.
     * @param spawnDataIn The Spawn Group data.
     * @param dataTag Extra Compound data.
     * @return The SpawnGroupData.
     */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            @NotNull ServerLevelAccessor worldIn, @NotNull DifficultyInstance difficultyIn, @NotNull MobSpawnType reason,
            @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        this.setTextColor(0xFFFFFF);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    /**
     * The file this npc is using for its behaviors.
     * @return The file name.
     */
    public String getBehaviorFile() {
        return this.entityData.get(BEHAVIOR_FILE);
    }

    public Behavior getCurrentBehavior() {
        return this.entityData.get(CURRENT_BEHAVIOR);
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    public String getTitle() {
        return this.entityData.get(TITLE);
    }

    public Component getTitleComponent() {
        if (!getTitle().isEmpty() && this.titleComponent == TextComponent.EMPTY)
            this.titleComponent = new TextComponent(getTitle()).withStyle(ChatFormatting.ITALIC);
        return this.titleComponent;
    }

    public boolean isTitleVisible() {
        return !this.getTitle().isEmpty() && !this.getTitle().isBlank();
    }

    public String getDialogue() {
        return this.entityData.get(DIALOGUE);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float horizontalScale = Math.max(scaleX, scaleZ);
        if (entityToRenderAs == null) {
            return POSES.getOrDefault(pose, Player.STANDING_DIMENSIONS).scale(horizontalScale, scaleY);
        }
        return entityToRenderAs.getDimensions(pose).scale(horizontalScale, scaleY);
    }

    /**
     * Get the hurt sound.
     * @return SoundEvent.
     */
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        if (damageSourceIn == DamageSource.ON_FIRE) {
            return SoundEvents.PLAYER_HURT_ON_FIRE;
        } else if (damageSourceIn == DamageSource.DROWN) {
            return SoundEvents.PLAYER_HURT_DROWN;
        } else {
            return damageSourceIn == DamageSource.SWEET_BERRY_BUSH ? SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH : SoundEvents.PLAYER_HURT;
        }
    }

    /**
     * Get the current trade offers.
     * @return TradeOffers.
     */
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

    public CompoundTag getScaleTag() {
        return this.entityData.get(SCALE);
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getScaleZ() {
        return scaleZ;
    }

    /**
     * Get this npc's block pos origin.
     * @return The block position.
     */
    public BlockPos getOrigin() {
        return this.entityData.get(ORIGIN);
    }

    public int getRadius() {
        return this.entityData.get(RADIUS);
    }

    /**
     * Get the sound source.
     * @return SoundSource.
     */
    public @NotNull SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    protected float getStandingEyeHeight(@NotNull Pose poseIn, @NotNull EntityDimensions sizeIn) {
        if (entityToRenderAs == null) {
            // Player#getStandingEyeHeight
            return switch (poseIn) {
                case SWIMMING, FALL_FLYING -> 0.4f;
                case SPIN_ATTACK -> 1.1F;
                case CROUCHING -> 1.27F;
                default -> 1.62F;
            };
        }
        return entityToRenderAs.getEyeHeightAccess(poseIn, sizeIn);
    }

    /**
     * Get the fast swim sound.
     * @return SoundEvent.
     */
    protected @NotNull SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    /**
     * Get the swim sound.
     * @return SoundEvent.
     */
    protected @NotNull SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    /**
     * Get the swim splash sound.
     * @return SoundEvent.
     */
    protected @NotNull SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    /**
     * Get the player this npc is talking to if any.
     * @return The player if any.
     */
    public @Nullable Player getTalkingPlayer() {
        return this.talkingPlayer;
    }

    /**
     * Get the target block to move to.
     * @return The Block Pos.
     */
    public BlockPos getTargetBlock() {
        return this.entityData.get(TARGET_BLOCK);
    }

    /**
     * Get the text color for dialogue.
     * @return The dialogue text color.
     */
    public int getTextColor() {
        return this.entityData.get(TEXTCOLOR);
    }

    /**
     * Get the texture of this npc.
     * @return The npc's texture.
     */
    public String getTexture() {
        return this.entityData.get(TEXTURE);
    }

    public boolean isTextureResourceLocation() {
        return this.entityData.get(IS_TEXTURE_RESOURCE_LOCATION);
    }

    /**
     * Get the timer of this entity.
     * @return The timer countdown.
     */
    public int getTriggerTimer() {
        return this.entityData.get(TRIGGER_TIMER);
    }

    /**
     * Handle player interacting with this NPC.
     * @param player The player interacting.
     * @param vec The direction of interaction.
     * @param hand InteractionHand.
     * @return The InteractionResult.
     */
    @Override
    public @NotNull InteractionResult interactAt(@NotNull Player player, @NotNull Vec3 vec, InteractionHand hand) {
        if (hand.equals(InteractionHand.MAIN_HAND)) {
            if (player.isAlive()) {
                IQuestCapability questCapability = QuestCapabilityProvider.getCapability(player);

                List<QuestInstance> markedForCompletion = new ArrayList<>();

                for (QuestInstance questInstance : questCapability.getAcceptedQuests()) {
                    if ((questInstance.getTurnInType() == QuestInstance.TurnInType.QuestGiver ||
                            questInstance.getTurnInType() == QuestInstance.TurnInType.NpcByUuid) &&
                            questInstance.getPickedUpFrom().equals(this.getUUID()) &&
                            questInstance.getQuest().canComplete()) {
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
                    // Test for behavior editing.
                    String behavior = getBehaviorFile();
                    if (!behavior.isEmpty() && player.getItemInHand(hand).getItem() instanceof BehaviorEditorItem) {
                        if (player.level.isClientSide) {
                            PacketDispatcher.sendToServer(new CRequestBehavior(behavior));
                            PacketDispatcher.sendToServer(new CRequestBehaviorEditor(behavior, this.getId()));
                            return InteractionResult.PASS;
                        }
                    }
                    //Test if dialogue editing.
                    String dialogueName = getDialogue();
                    // If we have a dialogue bound to the npc
                    if (!dialogueName.isEmpty()) {
                        if (!(player.getItemInHand(hand).getItem() instanceof NpcEditorItem || player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem)) {
                            // If the player doesn't have an NpcEditorItem in their hand, send them the dialogue and open the screen for it
                            if (player.level.isClientSide) {
                                PacketDispatcher.sendToServer(new CRequestDialogue(dialogueName, this.getId()));
                            }
                        } else if (!(player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem)) {
                            // Otherwise if they're opped, in creative mode, and sneaking, send them the dialogue editor and open the screen for it
                            if (player.hasPermissions(4) && player.isCreative() && player.isShiftKeyDown()) {
                                if (player.level.isClientSide) {
                                    PacketDispatcher.sendToServer(new CRequestDialogue(dialogueName, this.getId()));
                                    PacketDispatcher.sendToServer(new CRequestDialogueEditor(dialogueName, this.getId()));
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

    /**
     * Convert the current inventory to JSON.
     * @return JsonObject.
     */
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

    /**
     * Check if this entity is crouching.
     * @return boolean.
     */
    public boolean isCrouching() {
        return this.entityData.get(CROUCHING);
    }

    /**
     * Check if the goal of this action has been reached.
     * @return True if action completed.
     */
    public boolean isGoalReached() {
        return this.entityData.get(GOAL_REACHED);
    }

    /**
     * Check if the npc should continue moving to target block.
     * @return True if npc still moving.
     */
    public boolean isNotMovingToBlock() {
        if (isGoalReached() || getCurrentBehavior().isInvalid()) return true;
        Behavior behavior = getCurrentBehavior();
        Action action = behavior.getAction();
        switch (action.getActionType()) {
            case FOLLOW_PATH -> {
                return false;
            }
            case WANDER -> {
                return isTooFar();
            }
        }
        return true;
    }

    /**
     * Check if this entity is sitting.
     * @return boolean.
     */
    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    /**
     * Check if this entity is slim.
     * @return boolean.
     */
    public boolean isSlim() {
        return this.entityData.get(SLIM);
    }

    public String getRenderer() {
        return this.entityData.get(RENDERER);
    }

    public CompoundTag getRendererTag() {
        CompoundTag tag = this.entityData.get(RENDERER_TAG);
        tag.remove("id");
        return tag;
    }

    public CompoundTag getActualRendererTag() {
        CompoundTag tag = getRendererTag();
        tag.putString("id", getRenderer());
        return tag;
    }

    public EntityType<?> getRendererType() {
        String renderer = getRenderer();
        return EntityType.byString(renderer).orElse(this.getType());
    }

    public boolean isTooFar() {
        return this.blockPosition().distManhattan(this.getTargetBlock()) > this.getRadius();
    }

    /**
     * Detect if the behavior has changed and refresh the goals.
     */
    public void refreshGoals() {
        if (level.isClientSide) return;
        // load behaviors]
        if (this.getBehaviorFile().isEmpty()) return;

        Behavior[] behaviors = BehaviorSavedData.getBehaviorSavedData(Objects.requireNonNull(getServer()), this.getBehaviorFile())
                .getBehaviorSavedData();
        if(getCurrentBehavior().isInvalid()) {
            for (Behavior behavior : behaviors) {
                if (behavior.isInitData()) {
                    this.applyBehavior(behavior);
                    break;
                }
            }
            return;
        }
        Behavior currentBehavior = getCurrentBehavior();

        // look for changes of the loaded behaviors.
        for (Behavior behavior : behaviors) {
            if (behavior.getName().equals(currentBehavior.getName()) && !behavior.equals(currentBehavior)) {
                this.applyBehavior(behavior);
                break;
            }
        }
    }

    /**
     * Register the goals for the entity.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new TalkWithPlayerGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    /**
     * Check if this entity should be de-spawned if far away.
     * @param distanceToClosestPlayer The distance.
     * @return False.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * Reset the current behavior so the next time refreshGoals is called, it will use the init behavior.
     */
    public void resetBehavior() {
        ClientBehaviorUtil.loadBehavior(this.getBehaviorFile());
        if (ClientBehaviorUtil.currentBehavior != null) {
            Behavior[] behaviors = Behavior.multipleFromJSONObject(ClientBehaviorUtil.currentBehavior);

            BehaviorSavedData savedData = BehaviorSavedData.getBehaviorSavedData(Objects.requireNonNull(getServer()), this.getBehaviorFile());
            savedData.setBehaviors(behaviors);
            savedData.setDirty();
        }
        setCurrentBehavior(new Behavior());
        refreshGoals();
    }

    /**
     * Set behavior file.
     * @param s The filename.
     */
    public void setBehaviorFile(String s) {
        this.entityData.set(BEHAVIOR_FILE, s);
    }

    /**
     * Set crouching.
     * @param b True if crouching.
     */
    public void setCrouching(boolean b) {
        this.entityData.set(CROUCHING, b);
        if (b) {
            this.setPose(Pose.CROUCHING);
        } else {
            this.setPose(isSitting() ? Pose.SPIN_ATTACK : Pose.STANDING);
        }
    }

    /**
     * Set the current behavior.
     * @param s The behavior tag.
     */
    public void setCurrentBehavior(Behavior s) {
        this.entityData.set(CURRENT_BEHAVIOR, s);
    }

    /**
     * Set the custom attributes of this npc.
     * @return Attributes builder.
     */
    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    public void setTitle(String s) {
        this.entityData.set(TITLE, s);
        this.titleComponent = new TextComponent(s).withStyle(ChatFormatting.ITALIC);
    }

    /**
     * Set the dialogue of this entity.
     * @param s New dialogue.
     */
    public void setDialogue(String s) {
        this.entityData.set(DIALOGUE, s);
    }

    /**
     * Set if the goal has been reached.
     * @param goalReached New dialogue.
     */
    public void setGoalReached(boolean goalReached) {
        this.entityData.set(GOAL_REACHED, goalReached);
        if (goalReached) {
            trigger(Trigger.TriggerType.ACTION_FINISH_TRIGGER);
        }
    }

    /**
     * Set the radius for NPC behaviors.
     * @param radius The radius.
     */
    public void setRadius(int radius) {
        this.entityData.set(RADIUS, radius);
    }

    /**
     * Set the radius of the npc. In certain behaviors, the npc will be teleported back to this position if it moves too
     * far away.
     * @param pos The position.
     */
    public void setOrigin(BlockPos pos) {
        this.entityData.set(ORIGIN, pos);
    }

    /**
     * Set sitting.
     * @param b True to sit.
     */
    public void setSitting(boolean b) {
        this.entityData.set(SITTING, b);
        if (b) {
            this.setPose(Pose.SPIN_ATTACK);
        } else {
            this.setPose(isCrouching() ? Pose.CROUCHING : Pose.STANDING);
        }
    }

    /**
     * Set the character model to slim.
     * @param b The boolean.
     */
    public void setSlim(boolean b) {
        this.entityData.set(SLIM, b);
    }

    /**
     * Set the player this npc is talking to.
     * @param player The player.
     */
    public void setTalkingPlayer(@Nullable Player player) {
        this.talkingPlayer = player;
    }

    /**
     * Set the target block to `block`.
     * @param block The new target.
     */
    public void setTargetBlock(BlockPos block) {
        this.entityData.set(TARGET_BLOCK, block);
    }

    /**
     * Set the text color of this npc.
     * @param i The text color.
     */
    public void setTextColor(int i) {
        this.entityData.set(TEXTCOLOR, i);
    }

    /**
     * Set the texture of this npc. works best to provide url to image.
     * @param s The texture.
     */
    public void setTexture(String s) {
        this.entityData.set(TEXTURE, s);
    }

    public void setIsTextureResourceLocation(boolean b) {
        this.entityData.set(IS_TEXTURE_RESOURCE_LOCATION, b);
    }

    /**
     * Set the trade offers of this npc.
     * @param tradeOffers The trade offers.
     */
    @OnlyIn(Dist.CLIENT)
    public void setTradeOffers(@Nullable TradeOffers tradeOffers) {
        this.tradeOffers = tradeOffers;
    }

    public void setScale(float x, float y, float z) {
        if (x < 0.1f || y < 0.1f || z < 0.1f ||
            x > 15 || y > 15 || z > 15) return;

        CompoundTag tag = getScaleTag();
        tag.putFloat("x", x);
        tag.putFloat("y", y);
        tag.putFloat("z", z);
        this.entityData.set(SCALE, tag);
        scaleX = x;
        scaleY = y;
        scaleZ = z;

        refreshDimensions();
    }

    public void setRenderer(String renderer) {
        this.entityData.set(RENDERER, renderer);
        setEntityToRenderAs(getActualRendererTag());
    }

    public void setRenderer(EntityType<?> rendererType) {
        String renderer = EntityType.getKey(rendererType).toString();
        this.entityData.set(RENDERER, renderer);
        setEntityToRenderAs(getActualRendererTag());
    }

    public void setRendererTag(CompoundTag tag) {
        tag.remove("id");
        this.entityData.set(RENDERER_TAG, tag);
        setEntityToRenderAs(getActualRendererTag());
    }

    private void setEntityToRenderAs(CompoundTag tag) {
        if (this.getRendererType().equals(this.getType())) {
            this.entityToRenderAs = null;
            this.previousRendererTag = null;
            this.entityData.set(RENDERER_TAG, new CompoundTag());
            refreshDimensions();
            return;
        }

        Optional<Entity> createOptional = EntityType.create(tag, this.level);
        if (createOptional.isPresent()) {
            previousRendererTag = tag;
            this.entityToRenderAs = (LivingEntity) createOptional.get();
            setRenderedEntityItems();
            refreshDimensions();
        }
    }

    public void setRenderedEntityItems() {
        if (entityToRenderAs != null) {
            entityToRenderAs.setItemSlot(EquipmentSlot.MAINHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
            entityToRenderAs.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.OFFHAND));
            entityToRenderAs.setItemSlot(EquipmentSlot.HEAD, this.getItemBySlot(EquipmentSlot.HEAD));
            entityToRenderAs.setItemSlot(EquipmentSlot.CHEST, this.getItemBySlot(EquipmentSlot.CHEST));
            entityToRenderAs.setItemSlot(EquipmentSlot.LEGS, this.getItemBySlot(EquipmentSlot.LEGS));
            entityToRenderAs.setItemSlot(EquipmentSlot.FEET, this.getItemBySlot(EquipmentSlot.FEET));
        }
    }

    public LivingEntity getEntityToRenderAs() {
        if (entityToRenderAs == null ||
                entityToRenderAs.getType() != this.getRendererType() ||
                !getActualRendererTag().equals(previousRendererTag)) {
            setEntityToRenderAs(getActualRendererTag());
        }
        return entityToRenderAs;
    }

    /**
     * Set the countdown for a trigger to be called.
     * @param timer The time in seconds.
     */
    public void setTriggerTimer(int timer) {
        this.entityData.set(TRIGGER_TIMER, timer);
    }

    /**
     * Check if Player is unable to damage this entity.
     * @param entityIn The player.
     * @return True.
     */
    @Override
    public boolean skipAttackInteraction(@NotNull Entity entityIn) {
        if (entityIn instanceof Player player) {
            if (player.hasPermissions(4) && player.isCreative() && player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof NpcEditorItem) {
                this.kill();
            }
        }
        return true;
    }

    /**
     * Called every tick.
     */
    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            if (!level.isClientSide) {
                if (this.getOrigin().equals(BlockPos.ZERO)) {
                    this.setOrigin(this.blockPosition());
                }
                triggerTickCounter++;
                if (triggerTickCounter > 20) {
                    //long time = this.level.getDayTime() % 24000L;

                    int countDown = getTriggerTimer();
                    if (countDown > 0) {
                        this.setTriggerTimer(--countDown);
                        if (countDown == 0) {
                            this.trigger(Trigger.TriggerType.TIMER_TRIGGER);
                        }
                    }
                    triggerTickCounter = 0;
                }
                if (isNotMovingToBlock()) {
                    teleportCounter++;
                    if (teleportCounter > MAX_TELEPORT_COUNTER) {
                        this.teleportCounter = 0;
                        BlockPos origin = getOrigin();
                        if (!this.blockPosition().closerThan(origin, 1.0D)) {
                            this.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
                        }
                    }
                }
            }
            else {
                setRenderedEntityItems();
            }
        }
    }

    /**
     * Trigger the behavior.
     *
     * @param triggerName The name of the trigger.
     */
    public void trigger(String triggerName) {
        Behavior behavior = getCurrentBehavior();
        String nextBehaviorName = null;
        for (Trigger trigger : behavior.getTriggers()) {
            if (trigger.getName().equals(triggerName)) {
                nextBehaviorName = trigger.getNextBehaviorName();
                break;
            }
        }
        if (nextBehaviorName == null) {
            return;
        }

        for (Behavior nextBehavior : behavior.getChildren()) {
            if (nextBehavior.getName().equals(nextBehaviorName)) {
                this.applyBehavior(nextBehavior);
                return;
            }
        }
    }

    /**
     * Trigger the behavior.
     *
     * @param triggerType The type of the trigger.
     */
    public void trigger(Trigger.TriggerType triggerType) {
        Behavior behavior = getCurrentBehavior();
        String nextBehaviorName = null;
        for (Trigger trigger : behavior.getTriggers()) {
            if (trigger.getType() == triggerType) {
                nextBehaviorName = trigger.getNextBehaviorName();
                break;
            }
        }
        if (nextBehaviorName == null) {
            return;
        }

        for (Behavior nextBehavior : behavior.getChildren()) {
            if (nextBehavior.getName().equals(nextBehaviorName)) {
                this.applyBehavior(nextBehavior);
                return;
            }
        }
    }

}
