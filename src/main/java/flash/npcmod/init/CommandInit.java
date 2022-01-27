package flash.npcmod.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.Main;
import flash.npcmod.commands.Command;
import flash.npcmod.commands.DialoguesCommand;
import flash.npcmod.commands.FunctionsCommand;
import flash.npcmod.commands.QuestsCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

import java.util.function.Supplier;

import static net.minecraft.command.Commands.literal;

public class CommandInit {

  public static FunctionsCommand FUNCTIONS_COMMAND;
  public static QuestsCommand QUESTS_COMMAND;
  public static DialoguesCommand DIALOGUES_COMMAND;

  public static void registerCommands(CommandDispatcher<CommandSource> dispatcher, Commands.EnvironmentType env) {
    FUNCTIONS_COMMAND = registerCommand(FunctionsCommand::new, dispatcher, env);
    QUESTS_COMMAND = registerCommand(QuestsCommand::new, dispatcher, env);
    DIALOGUES_COMMAND = registerCommand(DialoguesCommand::new, dispatcher, env);
  }

  public static <T extends Command> T registerCommand(Supplier<T> supplier, CommandDispatcher<CommandSource> dispatcher, Commands.EnvironmentType env) {
    T command = supplier.get();

    if (!command.isDedicatedServerOnly() || env == Commands.EnvironmentType.DEDICATED || env == Commands.EnvironmentType.ALL) {
      LiteralArgumentBuilder<CommandSource> builder = literal(command.getName());
      builder.requires((sender) -> sender.hasPermissionLevel(command.getRequiredPermissionLevel()));
      command.build(builder);
      dispatcher.register(literal(Main.MODID).then(builder));
    }

    return command;
  }
}
