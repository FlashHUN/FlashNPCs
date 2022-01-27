package flash.npcmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;

public class DialoguesCommand extends Command {
  @Override
  public String getName() {
    return "dialogues";
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
    List<MutableComponent> dialogues = new ArrayList<>();
    List<String> fileNames = CommonDialogueUtil.readAllDialogueFileNames();
    fileNames.forEach(s -> {
      dialogues.add(new TextComponent(s).setStyle(Style.EMPTY)
      );
    });

    TextComponent questsComponent = new TextComponent("List of Dialogues: ");
    for (int i = 0; i < dialogues.size(); i++) {
      questsComponent.append(ComponentUtils.wrapInSquareBrackets(dialogues.get(i)).withStyle(ChatFormatting.GREEN));
      if (i < dialogues.size()-1)
        questsComponent.append(", ");
    }
    source.sendSuccess(questsComponent, false);
    return dialogues.size();
  }
}
