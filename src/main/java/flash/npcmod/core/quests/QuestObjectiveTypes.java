package flash.npcmod.core.quests;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import flash.npcmod.Main;
import flash.npcmod.core.pathing.Path;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

import static flash.npcmod.core.ItemUtil.stackToString;

public class QuestObjectiveTypes {

  public static class GatherObjective extends QuestObjective {
    private final ItemStack itemStack;

    public GatherObjective(int id, String name, ItemStack itemStack, int amount) {
      super(id, name, ObjectiveType.Gather, amount);
      this.itemStack = itemStack;
    }

    @Override
    public ItemStack getObjective() {
      return itemStack;
    }

    @Override
    public String primaryToString() {
      return stackToString(itemStack);
    }

    @Override
    public GatherObjective copy() {
      GatherObjective copy = new GatherObjective(getId(), getName(), itemStack, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static class KillObjective extends QuestObjective {
    private final String livingEntityKey;

    public KillObjective(int id, String name, String livingEntityKey, int amount) {
      super(id, name, ObjectiveType.Kill, amount);
      this.livingEntityKey = livingEntityKey;
    }

    @Override
    public String getObjective() {
      return livingEntityKey;
    }

    @Override
    public String primaryToString() {
      return livingEntityKey;
    }

    @Override
    public KillObjective copy() {
      KillObjective copy = new KillObjective(getId(), getName(), livingEntityKey, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static class DeliverToEntityObjective extends QuestObjective {
    private final ItemStack itemStack;
    private final String livingEntityKey;

    public DeliverToEntityObjective(int id, String name, ItemStack itemStack, String livingEntityKey, int amount) {
      super(id, name, ObjectiveType.DeliverToEntity, amount);
      this.itemStack = itemStack;
      this.livingEntityKey = livingEntityKey;
    }

    @Override
    public ItemStack getObjective() {
      return itemStack;
    }

    @Override
    public String getSecondaryObjective() {
      return livingEntityKey;
    }

    @Override
    public String primaryToString() {
      return stackToString(itemStack);
    }

    @Override
    public String secondaryToString() {
      return livingEntityKey;
    }

    @Override
    public DeliverToEntityObjective copy() {
      DeliverToEntityObjective copy = new DeliverToEntityObjective(getId(), getName(), itemStack, livingEntityKey, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static class DeliverToLocationObjective extends QuestObjective {
    private final ItemStack itemStack;
    private final BlockPos[] area;

    public DeliverToLocationObjective(int id, String name, ItemStack itemStack, BlockPos[] area, int amount) {
      super(id, name, ObjectiveType.DeliverToLocation, amount);
      this.itemStack = itemStack;
      this.area = area;
    }

    @Override
    public ItemStack getObjective() {
      return itemStack;
    }

    @Override
    public BlockPos[] getSecondaryObjective() {
      return area;
    }

    @Override
    public String primaryToString() {
      return stackToString(itemStack);
    }

    @Override
    public String secondaryToString() {
      return areaToString(area);
    }

    @Override
    public DeliverToLocationObjective copy() {
      DeliverToLocationObjective copy = new DeliverToLocationObjective(getId(), getName(), itemStack, area, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  // TODO NPC Pathing, figure out how this should work
  public static class EscortObjective extends QuestObjective {
    private final String npcName;
    private final Path path;

    public EscortObjective(int id, String name, String npcName, Path path) {
      super(id, name, ObjectiveType.Escort, 1);
      this.npcName = npcName;
      this.path = path;
      this.setShouldDisplayProgress(false);
    }

    @Override
    public String getObjective() {
      return npcName;
    }

    @Override
    public Path getSecondaryObjective() {
      return path;
    }

    @Override
    public String primaryToString() {
      return npcName;
    }

    @Nullable
    @Override
    public String secondaryToString() {
      return path.toString();
    }

    @Override
    public EscortObjective copy() {
      EscortObjective copy = new EscortObjective(getId(), getName(), npcName, path);
      copyTo(copy);
      return copy;
    }
  }

  public static class TalkObjective extends QuestObjective {
    private final String npcName;
    private final String dialogueNodeName;

    public TalkObjective(int id, String name, String npcName, String dialogueNodeName) {
      super(id, name, ObjectiveType.Talk, 1);
      this.npcName = npcName;
      this.dialogueNodeName = dialogueNodeName;
      this.setShouldDisplayProgress(false);
    }

    @Override
    public String getObjective() {
      return npcName;
    }

    @Override
    public String getSecondaryObjective() {
      return dialogueNodeName;
    }

    @Override
    public String primaryToString() {
      return npcName;
    }

    @Override
    public String secondaryToString() {
      return dialogueNodeName;
    }

    @Override
    public TalkObjective copy() {
      TalkObjective copy = new TalkObjective(getId(), getName(), npcName, dialogueNodeName);
      copyTo(copy);
      return copy;
    }
  }

  public static class FindObjective extends QuestObjective {
    private final BlockPos[] area;

    public FindObjective(int id, String name, BlockPos[] area) {
      super(id, name, ObjectiveType.Find, 1);
      this.area = area;
      this.setShouldDisplayProgress(false);
    }

    @Override
    public BlockPos[] getObjective() {
      return area;
    }

    @Override
    public String primaryToString() {
      return areaToString(area);
    }

    @Override
    public FindObjective copy() {
      FindObjective copy = new FindObjective(getId(), getName(), area);
      copyTo(copy);
      return copy;
    }
  }

  public static class UseOnEntityObjective extends QuestObjective {
    private final ItemStack itemStack;
    private final String livingEntityKey;

    public UseOnEntityObjective(int id, String name, ItemStack itemStack, String livingEntityKey, int amount) {
      super(id, name, ObjectiveType.UseOnEntity, amount);
      this.itemStack = itemStack;
      this.livingEntityKey = livingEntityKey;
    }

    @Override
    public ItemStack getObjective() {
      return itemStack;
    }

    @Override
    public String getSecondaryObjective() {
      return livingEntityKey;
    }

    @Override
    public String primaryToString() {
      return stackToString(itemStack);
    }

    @Override
    public String secondaryToString() {
      return livingEntityKey;
    }

    @Override
    public UseOnEntityObjective copy() {
      UseOnEntityObjective copy = new UseOnEntityObjective(getId(), getName(), itemStack, livingEntityKey, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static class UseOnBlockObjective extends QuestObjective {
    private final ItemStack itemStack;
    private final BlockState blockState;

    public UseOnBlockObjective(int id, String name, ItemStack itemStack, BlockState blockState, int amount) {
      super(id, name, ObjectiveType.UseOnBlock, amount);
      this.itemStack = itemStack;
      this.blockState = blockState;
    }

    @Override
    public ItemStack getObjective() {
      return itemStack;
    }

    @Override
    public BlockState getSecondaryObjective() {
      return blockState;
    }

    @Override
    public String primaryToString() {
      return stackToString(itemStack);
    }

    @Override
    public String secondaryToString() {
      String block = blockState.toString();
      return block.substring(6).replaceFirst("}", "");
    }

    @Override
    public UseOnBlockObjective copy() {
      UseOnBlockObjective copy = new UseOnBlockObjective(getId(), getName(), itemStack, blockState, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static class UseObjective extends QuestObjective {
    private final ItemStack itemStack;

    public UseObjective(int id, String name, ItemStack itemStack, int amount) {
      super(id, name, ObjectiveType.Use, amount);
      this.itemStack = itemStack;
    }

    @Override
    public ItemStack getObjective() {
      return itemStack;
    }

    @Override
    public String primaryToString() {
      return stackToString(itemStack);
    }

    @Override
    public UseObjective copy() {
      UseObjective copy = new UseObjective(getId(), getName(), itemStack, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static class ScoreboardObjective extends QuestObjective {
    private final String objectiveName;

    public ScoreboardObjective(int id, String name, String objectiveName, int amount) {
      super(id, name, ObjectiveType.Scoreboard, amount);
      this.objectiveName = objectiveName;
    }

    @Override
    public String getObjective() {
      return objectiveName;
    }

    @Override
    public String primaryToString() {
      return objectiveName;
    }

    @Override
    public ScoreboardObjective copy() {
      ScoreboardObjective copy = new ScoreboardObjective(getId(), getName(), objectiveName, getAmount());
      copyTo(copy);
      return copy;
    }
  }

  public static String entityToString(LivingEntity livingEntity) {
    return livingEntity.getEntityString();
  }

  public static String areaToString(BlockPos[] area) {
    StringBuilder sb = new StringBuilder();
    sb.append(area[0].getX()).append(";").append(area[0].getY()).append(";").append(area[0].getZ()).append(",");
    sb.append(area[1].getX()).append(";").append(area[1].getY()).append(";").append(area[1].getZ());
    return sb.toString();
  }

  public static LivingEntity entityFromString(String s) {
    String[] parts = s.split(":");
    return (LivingEntity) ForgeRegistries.ENTITIES.getValue(new ResourceLocation(parts[0], parts[1])).create(Main.PROXY.getWorld());
  }

  public static BlockPos[] areaFromString(String s) {
    String[] corners = s.split(",");
    if (corners.length != 2) return new BlockPos[]{ BlockPos.ZERO, BlockPos.ZERO };
    String[] corner1 = corners[0].split(";");
    String[] corner2 = corners[1].split(";");
    BlockPos corner1Pos, corner2Pos;
    try {
      corner1Pos = new BlockPos(Integer.parseInt(corner1[0]), Integer.parseInt(corner1[1]), Integer.parseInt(corner1[2]));
    } catch (Exception e) {
      corner1Pos = BlockPos.ZERO;
    }
    try {
      corner2Pos = new BlockPos(Integer.parseInt(corner2[0]), Integer.parseInt(corner2[1]), Integer.parseInt(corner2[2]));
    } catch (Exception e) {
      corner2Pos = BlockPos.ZERO;
    }
    return new BlockPos[]{ corner1Pos, corner2Pos };
  }

  public static BlockState blockStateFromString(String s) {
    try {
      return BlockStateArgument.blockState().parse(new StringReader(s)).getState();
    } catch (CommandSyntaxException e) {
      return Blocks.AIR.getDefaultState();
    }
  }

}
