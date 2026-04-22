package com.geendutchman.lhf_mudv2.execution.commandline;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureContainer;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.commandline.CommandHandler.PingCommandHandler;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

@Component(value = "commandline")
public record CommandLineGenerator(IFactory factory, ObjectProvider<ExitHandler> exitHandler,
        ObjectProvider<PingCommandHandler> pingHandler, ObjectProvider<DropCommand> dropCommand,
        ObjectProvider<GoCommand> goCommand, ObjectProvider<InventoryCommand> inventoryCommand,
        ObjectProvider<SayCommand> sayCommand, ObjectProvider<SeeCommand> seeCommand,
        ObjectProvider<StatusCommand> statusCommand, ObjectProvider<TakeCommand> takeCommand)
        implements Function<MessageContext, CommandLine> {

    @Command(name = "mud", description = "The base of all commands to do things here.", subcommands = {
            HelpCommand.class })
    private static record MudCommand() {
    }

    public static class RichOutputTypeConversionException extends TypeConversionException {
        private final RichOutput output;

        public RichOutputTypeConversionException(RichOutput out) {
            super(out.printIt());
            this.output = out;
        }

        public RichOutputTypeConversionException(String out) {
            super(out);
            this.output = RichOutput.builder().setSequenceName("Issue Converting Type").addString(out).build();
        }

        public RichOutput getOutput() {
            return this.output;
        }

    }

    protected static abstract class EntityNameConverter<T extends Entity> implements ITypeConverter<T> {

        public abstract String entityTypeName();

        public abstract MessageContext ctx();

        protected abstract T singleContextCheck(final String trimmed);

        protected abstract ImmutableSet<T> manyContextCheck(final String trimmed);

        @Override
        public T convert(String value) throws Exception {
            if (value == null || value.trim().length() < 3) {
                throw new TypeConversionException(
                        String.format("A %s name must be at least three letters long", this.entityTypeName()));
            }
            final String trimmed = value.trim();
            final T found = this.singleContextCheck(trimmed);
            if (found != null && found.name().toString().equalsIgnoreCase(trimmed)) {
                return found;
            }
            final ImmutableSet<T> many = this.manyContextCheck(trimmed);
            if (many == null || many.isEmpty()) {
                throw new TypeConversionException(
                        String.format("No %s found with a name starting with '%s'", this.entityTypeName(), trimmed));
            }
            if (many.size() == 1) {
                return many.asList().getFirst();
            } else {
                final RichOutput.Builder out = RichOutput.builder()
                        .setSequenceName(String.format("Specific %s Not Found", this.entityTypeName()))
                        .addString("Your search for a").addString(this.entityTypeName())
                        .addString("with a name that starts with").addString(String.format("\"%s\"", trimmed))
                        .addString("was not specific enough.");
                final RichOutput.Builder sub = RichOutput.builder().setSequenceName("Potential Matches")
                        .setElementSeparator(Optional.of(RichOutputElement.ofString("\n - ")));
                many.stream().limit(10).forEach(c -> sub.addTaggable(c));
                out.addOutput(sub.build());
                throw new RichOutputTypeConversionException(out.build());
            }
        }
    }

    @Override
    public CommandLine apply(final MessageContext t) {
        IFactory injectFactory = new IFactory() {

            @Override
            public <K> K create(Class<K> cls) throws Exception {
                try {
                    if (t != null && t.getClass().isAssignableFrom(cls)) {
                        @SuppressWarnings("unchecked")
                        K result = (K) t;
                        return result;
                    }
                    return factory.create(cls);
                } catch (Exception e) {
                    return CommandLine.defaultFactory().create(cls);
                }
            }

        };

        final CommandLine line = new CommandLine(new MudCommand(), injectFactory);
        pingHandler.ifAvailable(ping -> line.addSubcommand(ping));
        exitHandler.ifAvailable(exit -> line.addSubcommand(exit));
        dropCommand.ifAvailable(drop -> line.addSubcommand(drop));
        goCommand.ifAvailable(go -> line.addSubcommand(go));
        inventoryCommand.ifAvailable(inv -> line.addSubcommand(inv));
        sayCommand.ifAvailable(say -> line.addSubcommand(say));
        seeCommand.ifAvailable(see -> line.addSubcommand(see));
        statusCommand.ifAvailable(status -> line.addSubcommand(status));
        takeCommand.ifAvailable(take -> line.addSubcommand(take));

        if (t != null) {
            line.registerConverter(Creature.class, new EntityNameConverter<Creature>() {

                @Override
                public String entityTypeName() {
                    return "Creature";
                }

                @Override
                public MessageContext ctx() {
                    return t;
                }

                @Override
                protected Creature singleContextCheck(String trimmed) {
                    final MessageContext c = this.ctx();
                    if (c == null) {
                        return null;
                    }
                    return c.creature().orElse(null);
                }

                @Override
                protected ImmutableSet<Creature> manyContextCheck(String trimmed) {
                    final MessageContext c = this.ctx();
                    if (c == null) {
                        return ImmutableSet.of();
                    }
                    final CreatureQuery q = CreatureQuery.builder()
                            .adjustEntityQuery(e -> e.setNamePattern(Pattern.compile("^" + Pattern.quote(trimmed))))
                            .build();
                    final CreatureContainer result = c.queryCreatures(q);
                    return result != null ? result.creatures() : ImmutableSet.of();
                }

            });
            line.registerConverter(Item.class, new EntityNameConverter<Item>() {

                @Override
                public String entityTypeName() {
                    return "Item";
                }

                @Override
                public MessageContext ctx() {
                    return t;
                }

                @Override
                protected Item singleContextCheck(String trimmed) {
                    final MessageContext c = this.ctx();
                    if (c == null) {
                        return null;
                    }
                    return c.item().orElse(null);
                }

                @Override
                protected ImmutableSet<Item> manyContextCheck(String trimmed) {
                    final MessageContext c = this.ctx();
                    if (c == null) {
                        return ImmutableSet.of();
                    }
                    final ItemQuery q = ItemQuery.builder()
                            .adjustEntityQuery(e -> e.setNamePattern(Pattern.compile("^" + Pattern.quote(trimmed))))
                            .build();
                    final ItemContainer result = c.queryItems(q);
                    return result != null ? result.items() : ImmutableSet.of();
                }

            });
            line.registerConverter(Entity.class, new EntityNameConverter<Entity>() {

                @Override
                public String entityTypeName() {
                    return "Entity";
                }

                @Override
                public MessageContext ctx() {
                    return t;
                }

                @Override
                protected Entity singleContextCheck(String trimmed) {
                    return null;
                }

                @Override
                protected ImmutableSet<Entity> manyContextCheck(String trimmed) {
                    final MessageContext c = this.ctx();
                    if (c == null) {
                        return ImmutableSet.of();
                    }
                    final EntityQuery q = EntityQuery.builder()
                            .setNamePattern(Pattern.compile("^" + Pattern.quote(trimmed))).build();
                    final ImmutableSortedMap<IEntityID, Entity> result = c.queryEntities(q);
                    return result != null ? result.values().stream().collect(ImmutableSet.toImmutableSet())
                            : ImmutableSet.of();
                }

            });
        }
        return line;
    }

}
