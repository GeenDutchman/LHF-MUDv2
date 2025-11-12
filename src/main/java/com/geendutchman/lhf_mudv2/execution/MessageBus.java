package com.geendutchman.lhf_mudv2.execution;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.NoSuchElementException;
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

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.display.Taggable.Tag;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessorID;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

public interface MessageBus {
    public ImmutableMap<IEntityID, MessageProcessorID> entityToProcessor();

    public ImmutableBiMap<MessageProcessorID, MessageProcessor> processorIDToProcessor();

    public abstract void registerProcessor(MessageProcessor processor);

    public abstract void registerProcessorDefault(MessageProcessor processor, Taggable.Tag entityClass);

    public abstract void registerEntity(final Entity entity, final MessageProcessorID processorID);

    public abstract void registerEntity(final IEntityID id, final MessageProcessorID processorID);

    public abstract MessageProcessingResult publish(final Event event);

    public abstract MessageProcessingResult send(final Command command);

    public abstract class AbstractMessageBus implements MessageBus {
        final private ConcurrentMap<IEntityID, MessageProcessorID> entityToProcessor = new ConcurrentHashMap<>();
        final private ConcurrentMap<Taggable.Tag, MessageProcessorID> entityClassDefaults = new ConcurrentHashMap<>();
        final private HashBiMap<MessageProcessorID, MessageProcessor> processorIDToProcessor = HashBiMap.create();
        protected final Logger logger = Logger.getLogger(this.getClass().getName());
        final private MessageProcessingResult.Failed NO_RESULT = MessageProcessingResult.Failed("no result");

        @Autowired
        private Duration timing;

        protected AbstractMessageBus(@Autowired Duration timing) {
            this.timing = timing;
            logger.config("Initialized");
        }

        @Override
        public final ImmutableMap<IEntityID, MessageProcessorID> entityToProcessor() {
            return ImmutableMap.copyOf(this.entityToProcessor);
        }

        @Override
        public final ImmutableBiMap<MessageProcessorID, MessageProcessor> processorIDToProcessor() {
            return ImmutableBiMap.copyOf(this.processorIDToProcessor);
        }

        @Override
        public final void registerProcessorDefault(MessageProcessor processor, Tag entityClass) {
            if (processor == null) {
                throw new NullPointerException("Cannot register null processor");
            }
            this.processorIDToProcessor.put(processor.messageProcessorID(), processor);
            if (entityClass != null) {
                this.entityClassDefaults.put(entityClass, processor.messageProcessorID());
            }

        }

        @Override
        public void registerProcessor(MessageProcessor processor) {
            if (processor == null) {
                throw new NullPointerException("Cannot register null processor");
            }
            this.processorIDToProcessor.put(processor.messageProcessorID(), processor);
        }

        @Override
        public final void registerEntity(final Entity entity, final MessageProcessorID processorID) {
            if (entity == null) {
                throw new NullPointerException("Cannot register null entity");
            }
            this.registerEntity(entity.identifier(), processorID);
        }

        @Override
        public final void registerEntity(final IEntityID id, final MessageProcessorID processorID) {
            if (id == null) {
                throw new NullPointerException("Cannot register null entity id");
            }
            if (!this.processorIDToProcessor.containsKey(processorID)) {
                throw new NoSuchElementException(
                        String.format("No message processor available with id: %s", processorID));
            }
            this.entityToProcessor.put(id, processorID);
            logger.config(() -> String.format("Processor %s will now handle messages for %s", processorID, id));
        }

        @Override
        public MessageProcessingResult publish(Event event) {
            if (event == null) {
                return MessageProcessingResult.Failed("cannot publish null event");
            }

            final String logname = String.format("%s.event.%s.%s", this.logger.getName(),
                    event.getClass().getSimpleName(), event.uuid());
            return this.handle(event, logname);
        }

        @Override
        public MessageProcessingResult send(Command command) {
            if (command == null) {
                return MessageProcessingResult.Failed("cannot send null command");
            }

            final String logname = String.format("%s.command.%s.%s", this.logger.getName(),
                    command.getClass().getSimpleName(), command.uuid());
            return this.handle(command, logname);
        }

        protected abstract ExecutorService executor(String logname);

        private MessageProcessingResult handle(final Message message, final String logname) {
            if (message == null) {
                return MessageProcessingResult.Failed("cannot handle null message");
            }

            final Logger eventLogger = Logger.getLogger(logname);
            final MessageProcessor processor = this.processorIDToProcessor
                    .get(this.entityToProcessor.get(message.routing().destination()));
            if (processor == null) {
                final String noDestFound = String.format("No destination found for message %s, routing %s",
                        message.getClass().getSimpleName(), message.routing());
                eventLogger.warning(noDestFound);
                return MessageProcessingResult.Failed(noDestFound);
            }

            try (final ExecutorService executor = this.executor(logname)) {
                if (message instanceof Event asEvent) {
                    executor.submit(() -> {
                        MessageProcessingResult myResult = NO_RESULT;
                        try {
                            eventLogger.fine("Started processing event");
                            myResult = processor.process(asEvent);
                        } finally {
                            eventLogger.finer(String.format("Processing finished: %s", myResult));
                        }
                    });
                    return MessageProcessingResult.HANDLED;
                }
                return executor.submit(() -> {
                    MessageProcessingResult value = NO_RESULT;
                    try {
                        eventLogger.fine("Starting processing");
                        value = processor.process(message);
                        return value;
                    } finally {
                        final String logMessage = String.format("Processing finished: %s", value);
                        eventLogger.fine(logMessage);
                    }
                }).get(timing.toNanos(), TimeUnit.NANOSECONDS);
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
            eventLogger.severe("Should not have been able to reach here");
            this.logger.severe("Should not have been able to reach here");
            throw new IllegalStateException("Should not have been able to reach here");
        }

    }

    @Component
    @Primary
    public final class VirtualMessageBus extends AbstractMessageBus {

        public VirtualMessageBus(@Autowired Duration timing) {
            super(timing);
        }

        @Override
        protected ExecutorService executor(final String logname) {
            final ThreadFactory factory = Thread.ofVirtual().name(logname, 0).factory();
            return Executors.newThreadPerTaskExecutor(factory);
        }
    }

    @Component
    public final class QueuedMessageBus extends AbstractMessageBus {

        private final ExecutorService service;

        public QueuedMessageBus(@Autowired Duration timing) {
            super(timing);
            final ThreadFactory factory = Thread.ofVirtual().name(this.getClass().getName(), 0).factory();
            this.service = Executors.newSingleThreadExecutor(factory);
        }

        @Override
        protected ExecutorService executor(String logname) {
            return this.service;
        }
    }

}
