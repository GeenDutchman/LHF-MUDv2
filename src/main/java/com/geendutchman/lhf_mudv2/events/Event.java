package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.auto.value.AutoValue;

public abstract class Event {

    private final UUID uuid = UUID.randomUUID();

    public final UUID uuid() {
        return this.uuid;
    }

    public abstract Optional<RichOutput> description();

    @AutoValue
    public static abstract class EventRouting {
        public abstract URI destination();

        public abstract URI sender();

        public abstract URI replyTo();

        public abstract EventRoutingBuilder toBuilder();

        static EventRoutingBuilder builder() {
            return new AutoValue_Event_EventRouting.Builder();
        }

        @AutoValue.Builder
        public interface EventRoutingBuilder {
            public URI destination();

            public URI sender();

            public URI replyTo();

            public EventRoutingBuilder setDestination(URI destination);

            public default EventRoutingBuilder setDestination(EventProcessor processor) {
                return this.setDestination(processor.processorURI());
            }

            EventRoutingBuilder setSender(URI sender);

            default EventRoutingBuilder setSender(EventProcessor processor) {
                return this.setSender(processor.processorURI());
            }

            public EventRoutingBuilder setReplyTo(URI replyTo);

            public default EventRoutingBuilder setReplyTo(EventProcessor processor) {
                return this.setReplyTo(processor.processorURI());
            }

            public default EventRoutingBuilder setReplyToSender(URI sender) {
                return this.setSender(sender).setReplyTo(sender);
            }

            public default EventRoutingBuilder setReplyToSender(EventProcessor processor) {
                return this.setReplyToSender(processor.processorURI());
            }

            public default EventRoutingBuilder setDestinationAndSender(URI destination, URI sender) {
                return this.setDestination(destination).setReplyToSender(sender);
            }

            public default EventRoutingBuilder setDestinationAndSender(EventProcessor destination,
                    EventProcessor sender) {
                return this.setDestinationAndSender(destination.processorURI(), sender.processorURI());
            }

            public EventRouting build();
        }
    }

    public abstract EventRouting routing();

}
