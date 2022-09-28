package flash.npcmod.core;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class EntityUtil {

  private static String[] ENTITY_TYPES = new String[0];
  private static boolean hasLoadedEntities = false;

  public static void loadAllValidEntities(Level world) {
    List<EntityType<?>> entityTypes = new ArrayList<>();
    ForgeRegistries.ENTITIES.forEach(entityType -> {
      if (entityType.create(world) instanceof LivingEntity) {
        entityTypes.add(entityType);
      }
    });
    ENTITY_TYPES = entityTypes.stream().map(type -> EntityType.getKey(type).toString()).toArray(String[]::new);
    hasLoadedEntities = true;
  }

  public static boolean hasLoadedEntities() {
    return hasLoadedEntities;
  }

  public static String[] getEntityTypes() {
    return ENTITY_TYPES.clone();
  }

}
