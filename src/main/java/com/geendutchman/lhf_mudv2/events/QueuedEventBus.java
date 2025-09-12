package com.geendutchman.lhf_mudv2.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.repository.EntityResolver;
import com.geendutchman.lhf_mudv2.events.EventProcessor.ProcessingResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

@Service
final class QueuedEventBus implements EventBus {
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<URI, EventProcessor> processors = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Autowired
    private EntityResolver entityResolver;

    @Autowired
    private EventHandlerRegistry handlerRegistry;

    @Autowired
    private Duration timing;

    protected QueuedEventBus(@Autowired EntityResolver entityResolver, @Autowired EventHandlerRegistry handlerRegistry,
            @Autowired Duration timing) {
        Preconditions.checkNotNull(this.processors, "processor map must not be null");
        Preconditions.checkNotNull(handlerRegistry, "handler registry must not be null");
        Preconditions.checkNotNull(entityResolver, "entity resolver must not be null");
        this.entityResolver = entityResolver;
        this.handlerRegistry = handlerRegistry;
        this.timing = timing;
        this.logger.info("Starting eventBus worker");
        Thread.ofVirtual().name("eventBus").start(this::processEvents);
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
        if (event == null) {
            return;
        }
        this.queue.offer(event);
    }

    private void processEvents() {
        Preconditions.checkNotNull(this.queue, "queue must not be null");
        Preconditions.checkNotNull(this.processors, "processor map must not be null");
        Preconditions.checkNotNull(this.handlerRegistry, "handler registry must not be null");
        Preconditions.checkNotNull(this.entityResolver, "entity resolver must not be null");
        while (true) {
            try {
                final Event event = this.queue.take();
                if (event == null) {
                    continue;
                }
                final String threadname = String.format("%s.event.%s.%s", this.logger.getName(),
                        event.getClass().getSimpleName(), event.uuid());
                final Logger eventLogger = Logger.getLogger(threadname);
                final EventProcessor target = this.processors.get(event.routing().destination());
                if (target != null) {
                    eventLogger.finest("Directly addressed event");
                    switch (target.processEvent(event, this)) {
                    case ProcessingResult.Handled h -> {
                        // done
                    }
                    case ProcessingResult.Unhandled u -> {
                        final EventHandler<Event> handler = this.handlerRegistry.getHandler(event.getClass());
                        if (handler != null) {
                            Thread.ofVirtual().name(threadname).start(() -> handler.handle(event, target, this))
                                    .join(timing);
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
                            switch (entity.processEvent(event, this)) {
                            case ProcessingResult.Handled h -> {
                                // done
                            }
                            case ProcessingResult.Unhandled u -> {
                                final EventHandler<Event> handler = this.handlerRegistry.getHandler(event.getClass());
                                if (handler != null) {
                                    Thread.ofVirtual().name(threadname).start(() -> handler.handle(event, target, this))
                                            .join(timing);
                                } else {
                                    eventLogger.warning(String.format("No handler for %s -> %s, oh well",
                                            event.getClass().getSimpleName(), entity.getClass().getSimpleName()));
                                }
                            }
                            }
                        }

                    }
                }
            } catch (InterruptedException e) {
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
                break;
            }
        }
    }
}