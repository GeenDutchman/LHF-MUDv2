package com.geendutchman.lhf_mudv2.commands;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;

import com.geendutchman.lhf_mudv2.events.Event;
import com.google.common.collect.ImmutableSet;

public interface CommandProcessor {
    public URI processorURI();

    public Optional<URI> locale();

    public default ImmutableSet<UserCommand.UserCommandType> canHandle() {
        return ImmutableSet.of(); // TODO: figure out UserCommands
    }

    public default CommandResult processCommand(Command command) {
        return new CommandResult.CannotHandle(ImmutableSet.of());
    }

    public sealed interface CommandResult {
        public record CannotHandle(ImmutableSet<String> canHandle) implements CommandResult {
        }

        public record Handled(Event event) implements CommandResult {
            public Handled {
                if (event == null) {
                    throw new NullPointerException("command result response event cannot be null");
                }
            }
        }
    }

    /**
     * Represents how an command is to be handled via a specific command and
     * specific processor
     * 
     * @param <P> any type of MessageProcessor
     */
    @FunctionalInterface
    public interface CommandFunction<P extends CommandProcessor> extends BiFunction<Command, P, CommandResult> {
        /**
         * Processes the event with respect to the event processor
         * 
         * @param command   the command to be processed
         * @param processor the MessageProcessor if any further data is needed
         * @return any result that is generated
         */
        public abstract CommandResult apply(Command command, P processor);
    }
}
