package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.util.Optional;

public interface EventProcessor {
    public URI processorURI();

    public Optional<URI> locale();

    public default ProcessingResult processEvent(Event event, EventBus bus) {
        // give us the option
        return new ProcessingResult.Unhandled();
    }

    /**
     * Represents how an event is to be handled via a specific event and specific
     * processor
     * 
     * @param <P> any type of EventProcessor
     */
    @FunctionalInterface
    public interface EventFunction<P extends EventProcessor> {
        /**
         * Processes the event with respect to the event processor. The EventBus is
         * provided for any further events that are generated from processing this
         * event.
         * 
         * @param event     the event to be processed
         * @param bus       for forwarding any further events
         * @param processor the EventProcessor if any further data is needed
         * @return any result that is generated
         */
        ProcessingResult apply(Event event, EventBus bus, P processor);
    }

    public sealed interface ProcessingResult permits ProcessingResult.Handled, ProcessingResult.Unhandled {
        public record Handled() implements ProcessingResult {
        }

        public record Unhandled() implements ProcessingResult {
        }
    }

}
