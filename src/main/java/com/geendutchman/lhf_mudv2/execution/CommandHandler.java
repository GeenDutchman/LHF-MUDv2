package com.geendutchman.lhf_mudv2.execution;

import java.util.ArrayDeque;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Spec;

public interface CommandHandler {
    // @Spec
    // public CommandSpec spec();

    public boolean isEnabledFor(final CommandContext context);

    public IExecutionStrategy createRunner(final CommandContext context, final MessageBus bus);

    public CommandLine createCommandLine(final CommandContext context, final MessageBus bus);

    static final class Defer implements AutoCloseable {
        private final ArrayDeque<Runnable> onClose = new ArrayDeque<>();

        public void addLast(Runnable r) {
            if (r != null) {
                onClose.addLast(r);
            }
        }

        @Override
        public void close() {
            for (final Runnable r : onClose) {
                r.run();
            }
        }
    }

    @Command(name = "ping", description = "Tests connectivity")
    public record PingCommandHandler() implements CommandHandler {

        @Override
        public boolean isEnabledFor(CommandContext context) {
            return true;
        }

        @Override
        public IExecutionStrategy createRunner(CommandContext context, MessageBus bus) {
            return new IExecutionStrategy() {

                @Override
                public int execute(ParseResult parseResult) throws ExecutionException, ParameterException {
                    parseResult.asCommandLineList().getLast().getOut().println("pong");
                    // bus.publish(MessageContext.create(context.sender(), context.sender()),
                    // PlainEvent.asDescribed(RichOutput.builder().addString("pong").build()));
                    return 0;
                }

            };
        }

        @Override
        public CommandLine createCommandLine(CommandContext context, MessageBus bus) {
            CommandSpec spec = CommandSpec.create().name("ping");
            spec.usageMessage().description("Tests connectivity");
            return new CommandLine(spec).setExecutionStrategy(this.createRunner(context, bus));
        }

    }

    @Command(name = "exit", description = "Lets you leave Ibaif")
    public final class ExitCommandHandler implements CommandHandler {
        @Autowired
        private final ItemRepository itemRepository;
        @Autowired
        private final CreatureRepository creatureRepository;
        @Autowired
        private final RoomRepository roomRepository;

        public ExitCommandHandler(ItemRepository itemRepository, CreatureRepository creatureRepository,
                RoomRepository roomRepository) {
            this.itemRepository = itemRepository;
            this.creatureRepository = creatureRepository;
            this.roomRepository = roomRepository;
        }

        @Override
        public boolean isEnabledFor(CommandContext context) {
            return true;
        }

        @Override
        public CommandLine createCommandLine(CommandContext context, MessageBus bus) {
            CommandSpec spec = CommandSpec.create().name("exit");
            spec.usageMessage().description("Lets you leave Ibaif");
            spec.mixinStandardHelpOptions(true);
            // spec.addSubcommand("help", new HelpCommand());
            return spec.commandLine().setExecutionStrategy(this.createRunner(context, bus)).addSubcommand("help",
                    new HelpCommand());
        }

        @Override
        public IExecutionStrategy createRunner(final CommandContext context, final MessageBus bus) {
            final IExecutionStrategy strategy = new IExecutionStrategy() {

                @Override
                public int execute(ParseResult parseResult) throws ExecutionException, ParameterException {
                    Integer helpcode = CommandLine.executeHelpRequest(parseResult);
                    if (helpcode != null) {
                        return helpcode;
                    }
                    try (final Defer onClose = new Defer()) {
                        if (context.room().isPresent()
                                && context.sender().compareTo(context.room().get().identifier()) == 0) {
                            final Room room = context.room().get();
                            final RichOutput message = RichOutput.builder().addString("Cataclysm,").addTaggable(room)
                                    .addString("is exiting, taking you with it!").build();
                            Stream.concat(room.items().stream().map(i -> {
                                onClose.addLast(() -> itemRepository.remove(i));
                                return (Entity) i;
                            }), room.creatures().stream().map(c -> {
                                onClose.addLast(() -> creatureRepository.remove(c));
                                return (Entity) c;
                            })).filter(EntityQuery.builder().build()).forEach(entity -> {
                                bus.publish(MessageContext.create(room.identifier(), entity.identifier()),
                                        Event.PlainEvent.asDescribed(message));
                            });
                            onClose.addLast(() -> roomRepository.remove(room));
                        } else if (context.creature().isPresent()
                                && context.sender().compareTo(context.creature().get().identifier()) == 0) {
                            final Creature creature = context.creature().get();
                            bus.publish(MessageContext.create(creature.identifier(), creature.identifier()),
                                    Event.PlainEvent.asDescribed(
                                            RichOutput.builder().addString("Goodbye,").addTaggable(creature).build()));
                            final RichOutput message = RichOutput.builder().addString("Cataclysm,")
                                    .addTaggable(creature).addString("is exiting, taking you with them!").build();
                            creature.items().stream().forEach(i -> {
                                onClose.addLast(() -> creature.applyDelta(Creature.Delta.ofItemToRemove(i)));
                                onClose.addLast(() -> itemRepository.remove(i));
                                bus.publish(MessageContext.create(creature.identifier(), i.identifier()),
                                        Event.PlainEvent.asDescribed(message));
                            });
                            context.room().ifPresent(r -> {
                                r.applyDelta(Room.Delta.ofCreatureToRemove(creature));
                                bus.publish(MessageContext.create(r.identifier(), r.identifier()),
                                        PlainEvent.asDescribed(RichOutput.builder().addTaggable(creature)
                                                .addString("has exited Ibaif").build()));
                            });
                            onClose.addLast(() -> creatureRepository.remove(creature));
                        } else if (context.item().isPresent()
                                && context.sender().compareTo(context.item().get().identifier()) == 0) {
                            final Item item = context.item().get();
                            context.room().ifPresent(r -> {
                                r.applyDelta(Room.Delta.ofItemToRemove(item));
                            });
                            context.creature().ifPresent(c -> {
                                c.applyDelta(Creature.Delta.ofItemToRemove(item));
                            });
                            onClose.addLast(() -> itemRepository.remove(item));
                        } else {
                            bus.publish(MessageContext.create(context.sender(), context.sender()),
                                    PlainEvent.asDescribed(RichOutput.builder().addString(
                                            "Somehow, you can't exit right now. That is a heckin' huge problem!")
                                            .build()));
                            return 1;
                        }
                        // TODO: remove the commandline entity
                        return 0;

                    }

                }

            };

            return strategy;
        }

    }
}
