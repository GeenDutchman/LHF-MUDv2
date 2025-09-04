package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.geendutchman.lhf_mudv2.events.EventProcessor.ProcessingResult;
import com.google.common.collect.ImmutableSet;

@Service
final class QueuedEventBus implements EventBus {
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<URI, EventProcessor> processors = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger("eventBus");

    @Autowired
    private EventHandlerRegistry handlerRegistry;

    @Autowired
    private Duration timing;

    protected QueuedEventBus() {
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
        while (true) {
            try {
                final Event event = this.queue.take();
                final String threadname = String.format("event.%s", event.uuid());
                final EventProcessor target = this.processors.get(event.routing().destination());
                if (target != null) {
                    this.logger.finest("Directly addressed event");
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
                            this.logger.warning(String.format("No handler for %s -> %s, oh well",
                                    event.getClass().getSimpleName(), target.getClass().getSimpleName()));
                        }
                    }
                    }
                } else {
                    this.logger.warning("Broadcast not yet supported");
                    throw new UnsupportedOperationException("Broadcast not yet supported");
                }
            } catch (InterruptedException e) {
                this.logger.warning(() -> String.format("Thread interrupted: %s", e));
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}