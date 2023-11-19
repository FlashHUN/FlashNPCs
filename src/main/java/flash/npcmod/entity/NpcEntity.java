package flash.npcmod.entity;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.ItemUtil;
import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.goals.TalkWithPlayerGoal;
import flash.npcmod.init.EntityInit;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class NpcEntity extends PathfinderMob {

    public static String TYPE_STRING = EntityType.getKey(EntityInit.NPC_ENTITY.get()).toString();

    private static final EntityDataAccessor<String> TITLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> RENDERER_TAG = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> CROUCHING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DIALOGUE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<BlockPos> ORIGIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BLOCK_POS);

    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.3F);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TEXTCOLOR = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_TEXTURE_RESOURCE_LOCATION = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SCALE_X = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE_Y = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE_Z = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> COLLISION = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, Player.STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, SITTING_DIMENSIONS).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    public static final int MAX_OFFERS = 12;
    private static final int MAX_TELEPORT_COUNTER = 20 * 15; // 20 ticks (1 second) * amount of seconds
    @Nullable
    private Player talkingPlayer;
    private int teleportCounter;

    @Nullable
    private TradeOffers tradeOffers;
    private LivingEntity renderedEntity;
    private CompoundTag prevRenderedEntityTag;
    private Component titleComponent = TextComponent.EMPTY;

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        teleportCounter = 0;
        this.setPersistenceRequired();
    }

    protected NpcEntity(Level world) {
        this(EntityInit.NPC_ENTITY.get(), world);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("title", getTitle());
        compound.putString("dialogue", getDialogue());
        compound.putBoolean("nameVisibility", isCustomNameVisible());
        compound.putBoolean("slim", isSlim());
        compound.putInt("textColor", getTextColor());
        compound.putString("texture", getTexture());
        compound.putBoolean("is_texture_resource_loc", isTextureResourceLocation());
        compound.put("origin", NbtUtils.writeBlockPos(getOrigin()));
        compound.putBoolean("sitting", isSitting());
        compound.putBoolean("crouching", isCrouching());
        compound.put("renderer", getRenderedEntityTagWithId());
        compound.putFloat("scaleX", getScaleX());
        compound.putFloat("scaleY", getScaleY());
        compound.putFloat("scaleZ", getScaleZ());
        compound.putBoolean("collision", hasCollision());

        TradeOffers tradeOffers = this.getOffers();
        if (!tradeOffers.isEmpty()) {
            compound.put("Offers", tradeOffers.write());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setCrouching(compound.getBoolean("crouching"));
        setCustomNameVisible(compound.getBoolean("nameVisibility"));
        setTitle(compound.getString("title"));
        setDialogue(compound.getString("dialogue"));
        setOrigin(NbtUtils.readBlockPos(compound.getCompound("origin")));
        setSitting(compound.getBoolean("sitting"));
        setSlim(compound.getBoolean("slim"));
        setTextColor(compound.getInt("textColor"));
        setTexture(compound.getString("texture"));
        if (compound.contains("is_texture_resource_loc"))
            setIsTextureResourceLocation(compound.getBoolean("is_texture_resource_loc"));
        setRenderedEntityFromTag((CompoundTag) compound.get("renderer"));
        setScale(compound.getFloat("scaleX"), compound.getFloat("scaleY"), compound.getFloat("scaleZ"));
        setCollision(compound.getBoolean("collision"));

        if (compound.contains("Offers", 10)) {
            this.tradeOffers = new TradeOffers(compound.getCompound("Offers"));
        }
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", getName().getString());
        jsonObject.addProperty("nameVisibility", isCustomNameVisible());
        jsonObject.addProperty("dialogue", getDialogue());
        jsonObject.addProperty("title", getTitle());

        jsonObject.addProperty("texture", getTexture());
        jsonObject.addProperty("is_texture_resource_loc", isTextureResourceLocation());
        jsonObject.addProperty("textColor", getTextColor());
        jsonObject.addProperty("slim", isSlim());
        jsonObject.addProperty("pose", isSitting() ? "sitting" : isCrouching() ? "crouching" : "standing");
        jsonObject.addProperty("trades", getOffers().write().getAsString());
        jsonObject.addProperty("renderer", getRenderedEntityTagWithId().getAsString());
        JsonObject scale = new JsonObject();
        scale.addProperty("x", getScaleX());
        scale.addProperty("y", getScaleY());
        scale.addProperty("z", getScaleZ());
        jsonObject.add("scale", scale);
        jsonObject.addProperty("collision", hasCollision());

        jsonObject.add("inventory", inventoryToJson());

        return jsonObject;
    }

    public static NpcEntity fromJson(Level level, JsonObject jsonObject) {
        NpcEntity npcEntity = EntityInit.NPC_ENTITY.get().create(level);
        assert npcEntity != null;
        npcEntity.setCustomNameVisible(jsonObject.get("nameVisibility").getAsBoolean());
        npcEntity.setDialogue(jsonObject.get("dialogue").getAsString());
        npcEntity.setCustomName(new TextComponent(jsonObject.get("name").getAsString()));
        if (jsonObject.has("title"))
            npcEntity.setTitle(jsonObject.get("title").getAsString());
        npcEntity.setSlim(jsonObject.get("slim").getAsBoolean());
        npcEntity.setTexture(jsonObject.get("texture").getAsString());
        if (jsonObject.has("is_texture_resource_loc"))
            npcEntity.setIsTextureResourceLocation(jsonObject.get("is_texture_resource_loc").getAsBoolean());
        npcEntity.setTextColor(jsonObject.get("textColor").getAsInt());
        String pose = jsonObject.get("pose").getAsString();
        switch (pose) {
            case "sitting" -> npcEntity.setSitting(true);
            case "crouching" -> npcEntity.setCrouching(true);
        }
        if (jsonObject.has("scale")) {
            JsonObject scale = jsonObject.get("scale").getAsJsonObject();
            npcEntity.setScale(scale.get("x").getAsFloat(), scale.get("y").getAsFloat(), scale.get("z").getAsFloat());
        }
        if (jsonObject.has("collision")) {
            npcEntity.setCollision(jsonObject.get("collision").getAsBoolean());
        }
        npcEntity.tradeOffers = TradeOffers.read(jsonObject.get("trades").getAsString());
        try {
            npcEntity.setRenderedEntityFromTag(new TagParser(new StringReader(jsonObject.get("renderer").getAsString())).readStruct());
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

    @Override
    protected boolean canRide(@NotNull Entity entity) {
        return false;
    }

    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CROUCHING, false);
        this.entityData.define(DIALOGUE, "");
        this.entityData.define(ORIGIN, BlockPos.ZERO);
        this.entityData.define(TEXTCOLOR, 0xFFFFFF);
        this.entityData.define(TEXTURE, "");
        this.entityData.define(IS_TEXTURE_RESOURCE_LOCATION, false);
        this.entityData.define(SLIM, false);
        this.entityData.define(SITTING, false);
        this.entityData.define(RENDERER_TAG, new CompoundTag());
        this.entityData.define(SCALE_X, 1f);
        this.entityData.define(SCALE_Y, 1f);
        this.entityData.define(SCALE_Z, 1f);
        this.entityData.define(TITLE, "");
        this.entityData.define(COLLISION, true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            @NotNull ServerLevelAccessor worldIn, @NotNull DifficultyInstance difficultyIn, @NotNull MobSpawnType reason,
            @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        this.setTextColor(0xFFFFFF);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
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
        float horizontalScale = Math.max(getScaleX(), getScaleZ());
        if (renderedEntity == null) {
            return POSES.getOrDefault(pose, Player.STANDING_DIMENSIONS).scale(horizontalScale, getScaleY());
        }
        return renderedEntity.getDimensions(pose).scale(horizontalScale, getScaleY());
    }

    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        if (damageSourceIn == DamageSource.ON_FIRE) {
            return SoundEvents.PLAYER_HURT_ON_FIRE;
        } else if (damageSourceIn == DamageSource.DROWN) {
            return SoundEvents.PLAYER_HURT_DROWN;
        } else {
            return damageSourceIn == DamageSource.SWEET_BERRY_BUSH ? SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH : SoundEvents.PLAYER_HURT;
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

    public BlockPos getOrigin() {
        return this.entityData.get(ORIGIN);
    }

    public @NotNull SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    protected float getStandingEyeHeight(@NotNull Pose poseIn, @NotNull EntityDimensions sizeIn) {
        if (renderedEntity == null) {
            // Player#getStandingEyeHeight
            return switch (poseIn) {
                case SWIMMING, FALL_FLYING -> 0.4f;
                case SPIN_ATTACK -> 1.1F;
                case CROUCHING -> 1.27F;
                default -> 1.62F;
            };
        }
        return renderedEntity.getEyeHeightAccess(poseIn, sizeIn);
    }

    protected @NotNull SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    protected @NotNull SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    protected @NotNull SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    public @Nullable Player getTalkingPlayer() {
        return this.talkingPlayer;
    }

    public int getTextColor() {
        return this.entityData.get(TEXTCOLOR);
    }

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
                    String dialogueName = getDialogue();
                    if (!dialogueName.isEmpty()) {
                        if (!(player.getItemInHand(hand).getItem() instanceof NpcEditorItem || player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem)) {
                            if (player.level.isClientSide) {
                                PacketDispatcher.sendToServer(new CRequestDialogue(dialogueName, this.getId()));
                            }
                        } else if (!(player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem)) {
                            if (player.isCreative() && player.isShiftKeyDown()) {
                                if (player.level.isClientSide) {
                                    PacketDispatcher.sendToServer(new CRequestDialogue(dialogueName, this.getId()));
                                    PacketDispatcher.sendToServer(new CRequestDialogueEditor(dialogueName, this.getId()));
                                }
                            }
                        }
                    } else {
                        // If the NPC has trades, they don't have any dialogue, and we don't have the requirements to edit the npc in any way,
                        // open the trades gui for this npc
                        if (!((player.getItemInHand(hand).getItem() instanceof NpcEditorItem || player.getItemInHand(hand).getItem() instanceof NpcSaveToolItem))) {
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

    public boolean isCrouching() {
        return this.entityData.get(CROUCHING);
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public boolean isSlim() {
        return this.entityData.get(SLIM);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new TalkWithPlayerGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void setCrouching(boolean b) {
        this.entityData.set(CROUCHING, b);
        if (b) {
            this.setPose(Pose.CROUCHING);
        } else {
            this.setPose(isSitting() ? Pose.SPIN_ATTACK : Pose.STANDING);
        }
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    public void setTitle(String s) {
        this.entityData.set(TITLE, s);
        this.titleComponent = new TextComponent(s).withStyle(ChatFormatting.ITALIC);
    }

    public void setDialogue(String s) {
        this.entityData.set(DIALOGUE, s);
    }

    public void setOrigin(BlockPos pos) {
        this.entityData.set(ORIGIN, pos);
    }

    public void setSitting(boolean b) {
        this.entityData.set(SITTING, b);
        if (b) {
            this.setPose(Pose.SPIN_ATTACK);
        } else {
            this.setPose(isCrouching() ? Pose.CROUCHING : Pose.STANDING);
        }
    }

    public void setSlim(boolean b) {
        this.entityData.set(SLIM, b);
    }

    public void setTalkingPlayer(@Nullable Player player) {
        this.talkingPlayer = player;
    }

    public void setTextColor(int i) {
        this.entityData.set(TEXTCOLOR, i);
    }

    @OnlyIn(Dist.CLIENT)
    public void setTradeOffers(@Nullable TradeOffers tradeOffers) {
        this.tradeOffers = tradeOffers;
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity entityIn) {
        if (entityIn instanceof Player player) {
            if (PermissionHelper.hasPermission(player, PermissionHelper.EDIT_NPC) && player.isCreative() && player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof NpcEditorItem) {
                this.kill();
            }
        }
        return true;
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
                    if (!this.blockPosition().closerThan(origin, 1.0D)) {
                        this.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
                    }
                }
            }
            else {
                setRenderedEntityItems();
            }
        }
    }

    public boolean hasCollision() {
        return this.entityData.get(COLLISION);
    }

    public void setCollision(boolean collision) {
        this.entityData.set(COLLISION, collision);
    }

    @Override
    public boolean isPushable()
    {
        return hasCollision();
    }

    @Override
    protected void pushEntities()
    {
        if (hasCollision()) {
            super.pushEntities();
        }
    }

    // ====================== //
    //    RENDERER METHODS    //
    // ====================== //


    public float getScaleX() {
        return this.entityData.get(SCALE_X);
    }

    public float getScaleY() {
        return this.entityData.get(SCALE_Y);
    }

    public float getScaleZ() {
        return this.entityData.get(SCALE_Z);
    }

    public void setScale(float x, float y, float z) {
        if (x < 0.1f || y < 0.1f || z < 0.1f ||
                x > 15 || y > 15 || z > 15) return;

        this.entityData.set(SCALE_X, x);
        this.entityData.set(SCALE_Y, y);
        this.entityData.set(SCALE_Z, z);

        refreshDimensions();
    }

    public String getRenderedEntityTypeKey() {
        CompoundTag tag = getRenderedEntityTagWithId();
        return tag.contains("id") ? tag.getString("id") : TYPE_STRING;
    }

    public CompoundTag getRenderedEntityTagWithId() {
        return this.entityData.get(RENDERER_TAG);
    }

    public CompoundTag getRenderedEntityTagWithoutId() {
        CompoundTag tag = getRenderedEntityTagWithId().copy();
        tag.remove("id");
        return tag;
    }

    public LivingEntity getRenderedEntity() {
        if (renderedEntity == null
                || renderedEntity.getType() != this.getRenderedEntityType()
                || !getRenderedEntityTagWithId().equals(prevRenderedEntityTag)) {
            setRenderedEntityFromTag(getRenderedEntityTagWithId());
        }
        return renderedEntity;
    }

    public EntityType<?> getRenderedEntityType() {
        String entityTypeKey = getRenderedEntityTypeKey();
        if (entityTypeKey.equals(TYPE_STRING))
            return this.getType();
        return EntityType.byString(entityTypeKey).orElse(this.getType());
    }

    public void clearRenderedEntity() {
        setRenderedEntityFromTag(new CompoundTag());
    }

    public void setRenderedEntityFromTag(CompoundTag tag) {
        if (!tag.contains("id")
                || tag.getString("id").isBlank()
                || tag.getString("id").equals(TYPE_STRING)) {
            this.renderedEntity = null;
            this.prevRenderedEntityTag = null;
            this.entityData.set(RENDERER_TAG, new CompoundTag());
            refreshDimensions();
            return;
        }

        Optional<Entity> entityOpt = EntityType.create(tag, this.level);
        if (entityOpt.isPresent()) {
            prevRenderedEntityTag = tag;
            this.entityData.set(RENDERER_TAG, tag);
            renderedEntity = (LivingEntity) entityOpt.get();
            setRenderedEntityItems();
            refreshDimensions();
        }
    }

    public void setRenderedEntityItems() {
        if (renderedEntity != null) {
            renderedEntity.setItemSlot(EquipmentSlot.MAINHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
            renderedEntity.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.OFFHAND));
            renderedEntity.setItemSlot(EquipmentSlot.HEAD, this.getItemBySlot(EquipmentSlot.HEAD));
            renderedEntity.setItemSlot(EquipmentSlot.CHEST, this.getItemBySlot(EquipmentSlot.CHEST));
            renderedEntity.setItemSlot(EquipmentSlot.LEGS, this.getItemBySlot(EquipmentSlot.LEGS));
            renderedEntity.setItemSlot(EquipmentSlot.FEET, this.getItemBySlot(EquipmentSlot.FEET));
        }
    }

    public boolean isModelPartShown(PlayerModelPart part) {
        return true;
        // TODO return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & part.getMask()) == part.getMask();
    }

    public String getTexture() {
        return this.entityData.get(TEXTURE);
    }

    public boolean isTextureResourceLocation() {
        return this.entityData.get(IS_TEXTURE_RESOURCE_LOCATION);
    }

    public void setTexture(String s) {
        this.entityData.set(TEXTURE, s);
    }

    public void setIsTextureResourceLocation(boolean b) {
        this.entityData.set(IS_TEXTURE_RESOURCE_LOCATION, b);
    }
}
