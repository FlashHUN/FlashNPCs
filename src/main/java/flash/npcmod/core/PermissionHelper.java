package flash.npcmod.core;

import flash.npcmod.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

public class PermissionHelper {

    public static final PermissionNode<Boolean> CREATE_NPC = booleanNode("create.npc");
    public static final PermissionNode<Boolean> EDIT_NPC = booleanNode("edit.npc");
    public static final PermissionNode<Boolean> EDIT_DIALOGUE = booleanNode("edit.dialogue");
    public static final PermissionNode<Boolean> EDIT_QUEST = booleanNode("edit.quest");
    public static final PermissionNode<Boolean> EDIT_FUNCTION = booleanNode("edit.function");
    public static final PermissionNode<Boolean> EDIT_BEHAVIOR = booleanNode("edit.behavior");
    public static final PermissionNode<Boolean> SAVE_NPC = booleanNode("save_npc");

    public static boolean hasPermission(Player player, PermissionNode<Boolean> node) {
        if (player instanceof ServerPlayer serverPlayer) {
            return player.hasPermissions(4) || PermissionAPI.getPermission(serverPlayer, node);
        }
        return true;
    }

    private static PermissionNode<Boolean> booleanNode(String name) {
        return new PermissionNode<>(Main.MODID, name, PermissionTypes.BOOLEAN, (player, uuid, contexts) -> false);
    }

    public static void registerAll(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                CREATE_NPC,
                EDIT_NPC,
                EDIT_DIALOGUE,
                EDIT_QUEST,
                EDIT_FUNCTION,
                EDIT_BEHAVIOR,
                SAVE_NPC
        );
    }

}
