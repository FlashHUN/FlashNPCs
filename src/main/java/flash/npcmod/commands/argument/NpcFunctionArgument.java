package flash.npcmod.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.core.functions.FunctionUtil;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class NpcFunctionArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("some_function", "some_other_function");

    private NpcFunctionArgType type;

    private NpcFunctionArgument(NpcFunctionArgType type) {
        this.type = type;
    }

    public static NpcFunctionArgument function() {
        return new NpcFunctionArgument(NpcFunctionArgType.NO_DEFAULTS);
    }

    public static NpcFunctionArgument functionWithDefaults() {
        return new NpcFunctionArgument(NpcFunctionArgType.WITH_DEFAULTS);
    }

    public static String getName(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Stream<AbstractFunction> functions = FunctionUtil.FUNCTIONS.stream();
        if (type == NpcFunctionArgType.NO_DEFAULTS)
            functions = functions.filter(function -> !FunctionUtil.getDefaultFunctions().contains(function));

        return context.getSource() instanceof SharedSuggestionProvider
                ? SharedSuggestionProvider.suggest(functions.map(AbstractFunction::getName), builder)
                : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public enum NpcFunctionArgType {
        NO_DEFAULTS,
        WITH_DEFAULTS
    }
}
