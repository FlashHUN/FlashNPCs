package flash.npcmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;

public class BehaviorsCommand extends Command {
  @Override
  public String getName() {
    return "behaviors";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 4;
  }

  @Override
  public boolean isDedicatedServerOnly() {
    return false;
  }

  @Override
  public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.then(literal("list").executes(context -> list(context.getSource())));
  }

  private int list(CommandSourceStack source) {
    List<MutableComponent> behaviors = new ArrayList<>();
    List<String> fileNames = CommonBehaviorUtil.readAllBehaviorFileNames();
    fileNames.forEach(s -> behaviors.add(new TextComponent(s).setStyle(Style.EMPTY)
    ));

    TextComponent questsComponent = new TextComponent("List of Behaviors: ");
    for (int i = 0; i < behaviors.size(); i++) {
      questsComponent.append(ComponentUtils.wrapInSquareBrackets(behaviors.get(i)).withStyle(ChatFormatting.GREEN));
      if (i < behaviors.size()-1)
        questsComponent.append(", ");
    }
    source.sendSuccess(questsComponent, false);
    return behaviors.size();
  }
}
