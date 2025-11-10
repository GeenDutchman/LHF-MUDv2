package com.geendutchman.lhf_mudv2.execution;

import java.net.URI;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Preconditions;

public interface MessageProcessor {

    public record MessageProcessorID(UUID uuid) implements Comparable<MessageProcessorID> {
        public MessageProcessorID {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
        }

        @Override
        public int compareTo(MessageProcessorID o) {
            if (o == null) {
                throw new NullPointerException("cannot compare to nil Message Processor ID");
            }
            if (this == o) {
                return 0;
            }
            return this.uuid.compareTo(o.uuid);
        }

        public URI uri() {
            return UriComponentsBuilder.newInstance().pathSegment("{class}", "{uuid}").build("message_processor",
                    uuid.toString());
        }
    }

    public sealed interface MessageProcessingResult {
        public record Handled() implements MessageProcessingResult {
        }

        public final static Handled HANDLED = new Handled();

        public static Handled Handled() {
            return MessageProcessingResult.HANDLED;
        }

        public record Failed(String reason) implements MessageProcessingResult {
        }

        public static Failed Failed(String reason) {
            return new Failed(reason);
        }
    }

    public abstract MessageProcessorID messageProcessorID();

    public abstract MessageProcessingResult process(Message message);

    public default MessageProcessingResult process(Command command) {
        return this.process((Message) command);
    }

    public default MessageProcessingResult process(LHFCommand lhfCommand) {
        return this.process((Command) lhfCommand);
    }

    public default MessageProcessingResult process(UserCommand userCommand) {
        return this.process((Command) userCommand);
    }

    public default MessageProcessingResult process(Event event) {
        return this.process((Message) event);
    }
}
