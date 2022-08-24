package flash.npcmod;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.config.ConfigHolder;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.events.QuestEvents;
import flash.npcmod.init.CommandInit;
import flash.npcmod.init.EntityInit;
import flash.npcmod.init.ItemInit;
import flash.npcmod.network.PacketDispatcher;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
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
import org.jetbrains.annotations.NotNull;

@Mod(Main.MODID)
public class Main {

  // FIXME entity rotation is quite spinny in the renderer

  public static final String MODID = "flashnpcs";

  public static final Logger LOGGER = LogManager.getLogger("Flashs NPCs");

  // Proxies
  public static CommonProxy PROXY;

  public static final CreativeModeTab NPC_ITEMGROUP = new CreativeModeTab(Main.MODID) {
    @Override
    public @NotNull ItemStack makeIcon() {
      return new ItemStack(ItemInit.NPC_EDITOR.get());
    }
  };

  public Main() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    // Set up proxies
    PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);

    // Register the setup method for modloading
    modEventBus.addListener(this::setup);
    modEventBus.addListener(this::registerEntityAttributes);
    modEventBus.addListener(this::registerCapabilites);

    // Register the config
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHolder.COMMON_SPEC);

    // Register all the DeferredRegisters
    ItemInit.ITEMS.register(modEventBus);
    EntityInit.ENTITIES.register(modEventBus);

    MinecraftForge.EVENT_BUS.register(this);
    MinecraftForge.EVENT_BUS.register(new QuestEvents());
  }

  private void setup(final FMLCommonSetupEvent event) {
    // Register packets
    PacketDispatcher.registerMessages();
  }

  public void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(EntityInit.NPC_ENTITY.get(), NpcEntity.setCustomAttributes().build());
  }

  public void registerCapabilites(RegisterCapabilitiesEvent event) {
    event.register(IQuestCapability.class);
  }

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    CommandInit.registerCommands(event.getDispatcher(), event.getEnvironment());
  }
}
