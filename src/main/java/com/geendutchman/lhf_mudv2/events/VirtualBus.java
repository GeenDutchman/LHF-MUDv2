package com.geendutchman.lhf_mudv2.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.repository.EntityResolver;
import com.geendutchman.lhf_mudv2.events.EventProcessor.ProcessingResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

@Component
@Primary
public final class VirtualBus implements EventBus {
    private final ConcurrentMap<URI, EventProcessor> processors = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Autowired
    private EntityResolver entityResolver;

    @Autowired
    private EventHandlerRegistry handlerRegistry;

    @Autowired
    private Duration timing;

    VirtualBus(@Autowired EntityResolver entityResolver, @Autowired EventHandlerRegistry handlerRegistry,
            @Autowired Duration timing) {
        Preconditions.checkNotNull(this.processors, "processor map must not be null");
        Preconditions.checkNotNull(handlerRegistry, "handler registry must not be null");
        Preconditions.checkNotNull(entityResolver, "entity resolver must not be null");
        this.entityResolver = entityResolver;
        this.handlerRegistry = handlerRegistry;
        this.timing = timing;
    }

    @Override
    public ImmutableSet<URI> listProcessors() {
        return ImmutableSet.copyOf(this.processors.keySet());
    }

    @Override
    public void register(final EventProcessor processor) {
        if (processor == null) {
            return;
        }
        this.logger.finer(() -> String.format("registering processor %s", processor.processorURI()));
        this.processors.put(processor.processorURI(), processor);
    }

    @Override
    public void unregister(final EventProcessor processor) {
        if (processor == null) {
            return;
        }
        this.logger.finer(() -> String.format("unregistering processor %s", processor.processorURI()));
        this.processors.remove(processor.processorURI());
    }

    @Override
    public void publish(final Event event) {
        Preconditions.checkNotNull(this.processors, "processor map must not be null");
        Preconditions.checkNotNull(this.handlerRegistry, "handler registry must not be null");
        Preconditions.checkNotNull(this.entityResolver, "entity resolver must not be null");
        if (event == null) {
            return;
        }

        final String logname = String.format("%s.event.%s.%s", this.logger.getName(), event.getClass().getSimpleName(),
                event.uuid());
        final Logger eventLogger = Logger.getLogger(logname);
        ThreadFactory factory = Thread.ofVirtual().name(logname, 0).factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(factory)) {
            final EventProcessor target = this.processors.get(event.routing().destination());
            if (target != null) {
                eventLogger.finest("Directly addressed event");
                // we can do blocking calls with virtual threads
                switch (executor.submit(() -> target.processEvent(event)).get(timing.toNanos(), TimeUnit.NANOSECONDS)) {
                case ProcessingResult.Handled h -> {
                    // done
                }
                case ProcessingResult.Unhandled u -> {
                    final EventHandler<Event> handler = this.handlerRegistry.getHandler(event.getClass());
                    if (handler != null) {
                        executor.submit(() -> handler.handle(event, target, this)).get(timing.toNanos(),
                                TimeUnit.NANOSECONDS);
                    } else {
                        eventLogger.warning(String.format("No handler for %s -> %s, oh well",
                                event.getClass().getSimpleName(), target.getClass().getSimpleName()));
                    }

                }
                }
            } else {
                final SortedSet<Entity> targets = this.entityResolver.resolve(event.routing().destination());
                if (targets != null && targets.size() > 0) {
                    eventLogger.finest(() -> String.format("Event addressed %d targets", targets.size()));
                    for (final Entity entity : targets) {
                        if (entity == null) {
                            eventLogger.warning(() -> String.format("destination '%s' found null entity...skipping",
                                    event.routing().destination()));
                            continue;
                        }
                        switch (executor.submit(() -> entity.processEvent(event)).get(timing.toNanos(),
                                TimeUnit.NANOSECONDS)) {
                        case ProcessingResult.Handled h -> {
                            // done
                        }
                        case ProcessingResult.Unhandled u -> {
                            final EventHandler<Event> handler = this.handlerRegistry.getHandler(event.getClass());
                            if (handler != null) {
                                executor.submit(() -> handler.handle(event, target, this)).get(timing.toNanos(),
                                        TimeUnit.NANOSECONDS);
                            } else {
                                eventLogger.warning(String.format("No handler for %s -> %s, oh well",
                                        event.getClass().getSimpleName(), entity.getClass().getSimpleName()));
                            }

                        }
                        }
                    }
                } else {
                    eventLogger.warning(String.format("No destination found for event %s, routing %s",
                            event.getClass().getSimpleName(), event.routing()));
                }
            }

        } catch (NullPointerException | InterruptedException | ExecutionException | TimeoutException e) {
            this.logger.warning(() -> {
                StringWriter buffer = new StringWriter();
                PrintWriter writer = new PrintWriter(buffer);
                e.printStackTrace(writer);
                writer.flush();
                final String result = String.format("Thread interrupted: %s\n%s", e, buffer.toString());
                writer.close();
                return result;
            });
            Thread.currentThread().interrupt();
        }
    }

}
