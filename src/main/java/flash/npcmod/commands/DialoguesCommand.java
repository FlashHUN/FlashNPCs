package flash.npcmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.*;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.command.Commands.literal;

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
  public void build(LiteralArgumentBuilder<CommandSource> builder) {
    builder.then(literal("list").executes(context -> list(context.getSource())));
  }

  private int list(CommandSource source) {
    List<IFormattableTextComponent> dialogues = new ArrayList<>();
    List<String> fileNames = CommonDialogueUtil.readAllDialogueFileNames();
    fileNames.forEach(s -> {
      dialogues.add(new StringTextComponent(s).setStyle(Style.EMPTY));
    });

    StringTextComponent questsComponent = new StringTextComponent("List of Dialogues: ");
    for (int i = 0; i < dialogues.size(); i++) {
      questsComponent.appendSibling(TextComponentUtils.wrapWithSquareBrackets(dialogues.get(i)).mergeStyle(TextFormatting.GREEN));
      if (i < dialogues.size()-1)
        questsComponent.appendString(", ");
    }
    source.sendFeedback(questsComponent, false);
    return dialogues.size();
  }
}
