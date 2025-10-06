package com.geendutchman.lhf_mudv2.commands;

import java.net.URI;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;

public interface CommandProcessor {
    public URI commandProcessorURI();

    public Optional<URI> locale();

    public ImmutableSet<String> canHandle();

    public default CommandResult processCommand(Command command, CommandBus bus) {
        return new CommandResult.CannotHandle(ImmutableSet.of());
    }

    public sealed interface CommandResult {
        public record CannotHandle(ImmutableSet<String> canHandle) implements CommandResult {
        }

        public record Handled(Event event) implements CommandResult {
        }
    }
}
