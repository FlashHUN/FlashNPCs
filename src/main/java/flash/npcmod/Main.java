package flash.npcmod;

import flash.npcmod.capability.quests.QuestCapabilityAttacher;
import flash.npcmod.config.ConfigHolder;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.events.QuestEvents;
import flash.npcmod.init.CapabilityInit;
import flash.npcmod.init.CommandInit;
import flash.npcmod.init.EntityInit;
import flash.npcmod.init.ItemInit;
import flash.npcmod.network.PacketDispatcher;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Main.MODID)
public class Main {

  // The mod's id (namespace). The value here should match an entry in the META-INF/mods.toml file
  public static final String MODID = "flashnpcs";

  // Directly reference a log4j logger.
  public static final Logger LOGGER = LogManager.getLogger();

  // Proxies
  public static final CommonProxy PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);

  public static final CreativeModeTab NPC_ITEMGROUP = new CreativeModeTab(Main.MODID) {
    @Override
    public ItemStack makeIcon() {
      return new ItemStack(ItemInit.NPC_EDITOR.get());
    }
  };

  public Main() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    // Register the setup method for modloading
    modEventBus.addListener(this::setup);
    modEventBus.addListener(this::registerEntityAttributes);

    // Register the config
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHolder.COMMON_SPEC);

    // Register all the DeferredRegisters
    ItemInit.ITEMS.register(modEventBus);
    EntityInit.ENTITIES.register(modEventBus);

    MinecraftForge.EVENT_BUS.register(this);
    MinecraftForge.EVENT_BUS.register(new CapabilityInit());
    MinecraftForge.EVENT_BUS.register(new QuestEvents());
  }

  private void setup(final FMLCommonSetupEvent event) {
    // Register packets
    PacketDispatcher.registerMessages();
  }

  private void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(EntityInit.NPC_ENTITY.get(), NpcEntity.setCustomAttributes().build());
  }

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    CommandInit.registerCommands(event.getDispatcher(), event.getEnvironment());
  }
}
