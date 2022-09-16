package flash.npcmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;

import static net.minecraft.commands.Commands.argument;

public class SummonCommand extends Command {
    /**
     * Build the argument.
     * @param builder The LiteralArgumentBuilder.
     */
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(argument("position", BlockPosArgument.blockPos())
                .then(argument("name", StringArgumentType.string())
                        .then(argument("dialogue name", StringArgumentType.string())
                                .then(argument("behavior name", StringArgumentType.string())
                                        .then(argument("texture url", StringArgumentType.greedyString())
                                                .executes(context -> summon(
                                                                context.getSource(),
                                                                BlockPosArgument.getSpawnablePos(context, "position"),
                                                                StringArgumentType.getString(context, "name"),
                                                                StringArgumentType.getString(context, "dialogue name"),
                                                                StringArgumentType.getString(context, "behavior name"),
                                                                StringArgumentType.getString(context, "texture url")
                                                        )
                                                ))))));
    }

    /**
     * Get the name of the command.
     * @return summon.
     */
    @Override
    public String getName() {
        return "summon";
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
     * Summon an NPC.
     * @param source The CommandSourceStack.
     * @param position Where to summon the NPC.
     * @param name The name of the NPC.
     * @param dialogueName The Dialogue of the NPC.
     * @param behaviorName The Behavior of the NPC.
     * @param textureURL The texture url.
     * @return 1.
     */
    private int summon(
            CommandSourceStack source, BlockPos position, String name, String dialogueName, String behaviorName,
            String textureURL) {
        NpcEntity newNpc = EntityInit.NPC_ENTITY.get().create(source.getLevel());
        newNpc.setPos(position.getX(), position.getY(), position.getZ());
        source.getLevel().addFreshEntity(newNpc);
        newNpc.setTexture(textureURL);
        newNpc.setCustomName(new TextComponent(name));
        newNpc.setDialogue(dialogueName);
        newNpc.setBehaviorFile(behaviorName);

        TextComponent responseComponent = new TextComponent("Summoned NPC");
        source.sendSuccess(responseComponent, true);
        return 1;
    }
}
