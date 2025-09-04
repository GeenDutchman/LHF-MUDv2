package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.time.Duration;
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

import com.geendutchman.lhf_mudv2.events.EventProcessor.ProcessingResult;
import com.google.common.collect.ImmutableSet;

@Component
@Primary
public class VirtualBus implements EventBus {
    private final ConcurrentMap<URI, EventProcessor> processors = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger("eventBus");

    @Autowired
    private EventHandlerRegistry handlerRegistry;

    @Autowired
    private Duration timing;

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

        ThreadFactory factory = Thread.ofVirtual().name(String.format("event.%s", event.uuid()), 0).factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(factory)) {
            final EventProcessor target = this.processors.get(event.routing().destination());
            if (target != null) {
                this.logger.finest("Directly addressed event");
                // we can do blocking calls with virtual threads
                switch (executor.submit(() -> target.processEvent(event, this)).get(timing.toNanos(),
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
                        this.logger.warning(String.format("No handler for %s -> %s, oh well",
                                event.getClass().getSimpleName(), target.getClass().getSimpleName()));
                    }

                }
                }
            } else {
                // this.logger.finest("Broadcast everywhere");
                // List<Future<Boolean>> futures = executor
                // .invokeAll(this.processors.values().stream().map(processor -> new
                // Callable<Boolean>() {
                // @Override
                // public Boolean call() throws Exception {
                // processor.processEvent(event);
                // return true;
                // }

                // }).toList());
                // for (final Future<Boolean> future : futures) {
                // future.get(1, TimeUnit.MINUTES);
                // }
                this.logger.warning("Broadcast not yet supported");
                throw new UnsupportedOperationException("Broadcast not yet supported");
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            this.logger.warning(() -> String.format("Thread interrupted: %s", e));
            Thread.currentThread().interrupt();
        }
    }

}
