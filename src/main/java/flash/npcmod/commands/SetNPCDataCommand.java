package flash.npcmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SetNPCDataCommand extends Command {
    public static final String REGEX_PATTERN = "^[A-za-z\\d]{1,255}$";

    /**
     * Build the argument.
     * @param builder The LiteralArgumentBuilder.
     */
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(argument("entity", EntityArgument.entity())
                .then(literal("behavior")
                .then(argument("behavior name", StringArgumentType.string())
                .executes(context -> setBehavior(
                        context.getSource(),
                        EntityArgument.getEntity(context, "entity"),
                        StringArgumentType.getString(context, "behavior name")
                    )
                )
        )));

        builder.then(argument("entity", EntityArgument.entity())
                .then(literal("dialogue")
                .then(argument("dialogue name", StringArgumentType.string())
                .executes(context -> setDialogue(
                        context.getSource(),
                        EntityArgument.getEntity(context, "entity"),
                        StringArgumentType.getString(context, "dialogue name")
                    )
                )
        )));
    }

    /**
     * Get the name of the command.
     * @return summon.
     */
    @Override
    public String getName() {
        return "setNPCData";
    }

    /**
     * Get the required permission level.
     * @return The permission level.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    /**
     * Check if it only works on dedicated servers.
     * @return False.
     */
    @Override
    public boolean isDedicatedServerOnly() {
        return false;
    }

    /**
     * Set the new behavior for an npc.
     * @return The success.
     */
    private int setBehavior(CommandSourceStack source, Entity entity, String behaviorName) {
        if (behaviorName == null || !behaviorName.matches(REGEX_PATTERN)) {
            TextComponent responseComponent = new TextComponent("Behavior name must be alphanumeric characters only");
            source.sendFailure(responseComponent);
            return 0;
        }
        if (entity instanceof NpcEntity npcEntity) {
            npcEntity.setBehaviorFile(behaviorName);
            TextComponent responseComponent = new TextComponent("Set NPC's behavior name.");
            source.sendSuccess(responseComponent, true);
            return 1;
        }

        TextComponent responseComponent = new TextComponent("Entity was not a Flash NPC");
        source.sendFailure(responseComponent);
        return 0;
    }

    /**
     * Set the new dialogue for an npc.
     * @return The success.
     */
    private int setDialogue(CommandSourceStack source, Entity entity, String behaviorName) {
        if (behaviorName == null || !behaviorName.matches(REGEX_PATTERN)) {
            TextComponent responseComponent = new TextComponent("Dialogue name must be alphanumeric characters only");
            source.sendFailure(responseComponent);
            return 0;
        }
        if (entity instanceof NpcEntity npcEntity) {
            npcEntity.setDialogue(behaviorName);
            TextComponent responseComponent = new TextComponent("Set NPC's dialogue name.");
            source.sendSuccess(responseComponent, true);
            return 1;
        }

        TextComponent responseComponent = new TextComponent("Entity was not a Flash NPC");
        source.sendFailure(responseComponent);
        return 0;
    }
}
