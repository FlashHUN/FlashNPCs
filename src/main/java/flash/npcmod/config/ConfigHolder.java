package flash.npcmod.config;

import flash.npcmod.Main;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ConfigHolder {

  public static class Common {
    private static final List<String> DEFAULT_INVALID_COMMANDS = Arrays.asList(
        "/ban", "/ban-ip", "/deop", "/forceload", "/op", "/pardon", "/pardon-ip", "/save-off",
        "/setidletimeout", "/setworldspawn", "/stop", "/whitelist", "/"+Main.MODID
    );

    Predicate<Object> commandValidator = (text) -> text.getClass().isAssignableFrom(String.class) && String.valueOf(text).length() > 0;

    public final ForgeConfigSpec.ConfigValue<List<? extends String>> invalidCommands;

    public Common(ForgeConfigSpec.Builder builder) {
      builder.push("server");

      this.invalidCommands = builder.comment("Commands that will never be run by the mod. This is to prevent abuse.")
          .defineList("invalid_commands", DEFAULT_INVALID_COMMANDS, commandValidator);

      builder.pop();
    }

    public boolean isInvalidCommand(String command) {
      for (String invalid : invalidCommands.get()) {
        if (command.startsWith(invalid)) return true;
      }

      return false;
    }
  }

  public static final Common COMMON;
  public static final ForgeConfigSpec COMMON_SPEC;

  static {
    Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
    COMMON = commonSpecPair.getLeft();
    COMMON_SPEC = commonSpecPair.getRight();
  }

}
