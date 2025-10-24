package com.geendutchman.lhf_mudv2.events;

import java.net.URI;

public interface EventProcessor {
    public URI processorURI();

    public default ProcessingResult processEvent(Event event) {
        // give us the option
        return ProcessingResult.UNHANDLED;
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
         * Processes the event with respect to the event processor.
         * 
         * @param event     the event to be processed
         * @param processor the EventProcessor if any further data is needed
         * @return any result that is generated
         */
        ProcessingResult apply(Event event, P processor);
    }

    public sealed interface ProcessingResult permits ProcessingResult.Handled, ProcessingResult.Unhandled {
        public final static Handled HANDLED = new Handled();

        public record Handled() implements ProcessingResult {
        }

        public final static Unhandled UNHANDLED = new Unhandled();

        public record Unhandled() implements ProcessingResult {
        }
    }

}
