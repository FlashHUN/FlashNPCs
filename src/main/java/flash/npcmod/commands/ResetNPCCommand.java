package flash.npcmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;

import static net.minecraft.commands.Commands.argument;

public class ResetNPCCommand extends Command {
    /**
     * Build the argument.
     * @param builder The LiteralArgumentBuilder.
     */
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(argument("entity", EntityArgument.entity())
            .executes(context -> reset(
                    context.getSource(),
                    EntityArgument.getEntity(context, "entity")
                )
            )
        );
    }

    /**
     * Get the name of the command.
     * @return summon.
     */
    @Override
    public String getName() {
        return "resetAI";
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
     * Reset an npc's ai.
     * @param source The CommandSourceStack.
     * @param entity The npc.
     * @return 1.
     */
    private int reset(CommandSourceStack source, Entity entity) {
        if (entity instanceof NpcEntity) {
            ((NpcEntity) entity).resetBehavior();
            TextComponent responseComponent = new TextComponent("Reset NPC's AI");
            source.sendSuccess(responseComponent, true);
            return 1;
        }

        TextComponent responseComponent = new TextComponent("Entity was not a Flash NPC");
        source.sendFailure(responseComponent);
        return 0;
    }
}
