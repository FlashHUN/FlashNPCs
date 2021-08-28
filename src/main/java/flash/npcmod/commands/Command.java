package flash.npcmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

public abstract class Command {
  public abstract String getName();

  public abstract int getRequiredPermissionLevel();

  public abstract void build(LiteralArgumentBuilder<CommandSource> builder);

  public abstract boolean isDedicatedServerOnly();
}
