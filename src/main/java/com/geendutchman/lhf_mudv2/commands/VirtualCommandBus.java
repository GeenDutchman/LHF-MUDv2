package com.geendutchman.lhf_mudv2.commands;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.geendutchman.lhf_mudv2.commands.CommandProcessor.CommandResult;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.repository.EntityResolver;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

@Component
@Primary
public final class VirtualCommandBus implements CommandBus {
    private final ConcurrentMap<URI, CommandProcessor> processors = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Autowired
    private EntityResolver entityResolver;

    @Autowired
    private Duration timing;

    VirtualCommandBus(@Autowired EntityResolver entityResolver, @Autowired Duration timing) {
        Preconditions.checkNotNull(this.processors, "processor map must not be null");
        Preconditions.checkNotNull(entityResolver, "entity resolver must not be null");
        this.entityResolver = entityResolver;
        this.timing = timing;
    }

    @Override
    public ImmutableSet<URI> listCommandProcessors() {
        return ImmutableSet.copyOf(this.processors.keySet());
    }

    @Override
    public void registerCommandProcessor(CommandProcessor processor) {
        if (processor == null) {
            return;
        }
        this.logger.finer(() -> String.format("registering command processor %s", processor.commandProcessorURI()));
        this.processors.put(processor.commandProcessorURI(), processor);
    }

    @Override
    public void deregisterCommandProcessor(CommandProcessor processor) {
        if (processor == null) {
            return;
        }
        this.logger.finer(() -> String.format("deregistering command processor %s", processor.commandProcessorURI()));
        this.processors.remove(processor.commandProcessorURI());
    }

    protected CommandProcessor lookup(final URI dest) {
        final SortedSet<Entity> results = this.entityResolver.resolve(dest);
        if (results.size() > 1 || results.isEmpty()) {
            return null;
        }
        final Entity first = results.first();
        if (first != null && first instanceof CommandProcessor asCp) {
            return asCp;
        }
        return null;
    }

    @Override
    public CommandResult send(final Command command) {
        Preconditions.checkNotNull(this.processors, "processor map must not be null");
        Preconditions.checkNotNull(this.entityResolver, "entity resolver must not be null");
        if (command == null) {
            return new CommandResult.CannotHandle(ImmutableSet.of());
        }

        final String logname = String.format("%s.command.%s.%s", this.logger.getName(),
                command.getClass().getSimpleName(), command.uuid());

        final Logger commandLogger = Logger.getLogger(logname);
        // Thread.ofVirtual().name(logname)

        URI dest = command.routing().destination();
        ThreadFactory factory = Thread.ofVirtual().name(logname, 0).factory();

        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(factory)) {
            TreeSet<String> collectedCan = new TreeSet<>();
            LinkedHashSet<URI> whereBeen = new LinkedHashSet<>();

            while (dest != null && !whereBeen.contains(dest)) {
                whereBeen.add(dest);

                final CommandProcessor target = this.processors.getOrDefault(dest, this.lookup(dest));
                if (target == null) {
                    commandLogger.warning(String.format("processor '%s' not found", dest));
                    return new CommandResult.CannotHandle(ImmutableSet.copyOf(collectedCan));
                }

                CommandResult result = null;
                try {
                    result = executor.submit(() -> target.processCommand(command, this)).get(timing.toNanos(),
                            TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    commandLogger.log(Level.SEVERE, "was interrupted", e);
                    return new CommandResult.CannotHandle(ImmutableSet.copyOf(collectedCan));
                } catch (ExecutionException e) {
                    commandLogger.log(Level.SEVERE, "had problems executing", e);
                    return new CommandResult.CannotHandle(ImmutableSet.copyOf(collectedCan));
                } catch (TimeoutException e) {
                    commandLogger.log(Level.SEVERE, "timed out", e);
                    return new CommandResult.CannotHandle(ImmutableSet.copyOf(collectedCan));
                }

                switch (result) {
                case CommandResult.CannotHandle(ImmutableSet<String> canHandle) -> {
                    collectedCan.addAll(canHandle);
                    dest = target.locale().orElse(null);
                }
                case CommandResult.Handled(Event event) -> {
                    commandLogger.fine("command handled");
                    return result;
                }
                case null -> {
                    commandLogger.warning(String.format("processor '%s' returned a null!", dest));
                    dest = target.locale().orElse(null);
                }
                default -> {
                    commandLogger.warning(String.format("processor '%s' returned something weird: %s", dest, result));
                    dest = target.locale().orElse(null);
                }

                }
            }
            if (whereBeen.contains(dest)) {
                commandLogger.warning(String.format("loop detected in command chain", whereBeen));
            }
            return new CommandResult.CannotHandle(ImmutableSet.copyOf(collectedCan));
        }

    }

}
