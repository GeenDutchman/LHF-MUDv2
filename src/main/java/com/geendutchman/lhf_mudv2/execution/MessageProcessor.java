package com.geendutchman.lhf_mudv2.execution;

import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;

public interface MessageProcessor extends EventProcessor {

    public final static TsidFactory messageProcessorTsidFactory = TsidFactory
            .newInstance1024(Math.abs("message_processor".hashCode() % 1024));

    public record MessageProcessorID(Examinable.Name name, Tsid tsid) implements Comparable<MessageProcessorID> {
        public MessageProcessorID {
            Preconditions.checkNotNull(name, "name must not be null");
            Preconditions.checkNotNull(tsid, "tsid must not be null");
        }

        public static MessageProcessorID nextID(Examinable.Name name) {
            return new MessageProcessorID(name, messageProcessorTsidFactory.create());
        }

        @Override
        public int compareTo(MessageProcessorID o) {
            if (o == null) {
                throw new NullPointerException("cannot compare to nil Message Processor ID");
            }
            if (this == o) {
                return 0;
            }
            return this.tsid.compareTo(o.tsid);
        }

        public URI uri() {
            return UriComponentsBuilder.newInstance().pathSegment("{class}", "{name}", "{tsid}")
                    .build("message_processor", name, tsid.toString());
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

    public abstract MessageProcessingResult process(MessageContext context, Message message);

    public abstract MessageProcessingResult process(MessageContext context, Command command);

    public abstract MessageProcessingResult process(MessageContext context, LHFCommand lhfCommand);

    public abstract MessageProcessingResult process(MessageContext context, UserCommand userCommand);

}
