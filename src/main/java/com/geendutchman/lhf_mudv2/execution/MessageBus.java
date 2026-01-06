package com.geendutchman.lhf_mudv2.execution;

import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.display.Taggable.Tag;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessorID;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

public interface MessageBus {
    public ImmutableMap<IEntityID, MessageProcessorID> entityToProcessor();

    public ImmutableBiMap<MessageProcessorID, MessageProcessor> processorIDToProcessor();

    public ImmutableBiMap<Taggable.Tag, MessageProcessorID> entityClassDefaults();

    public default MessageProcessor processorForEntity(final IEntityID id) {
        if (id == null) {
            return null;
        }
        final ImmutableBiMap<MessageProcessorID, MessageProcessor> idToProc = this.processorIDToProcessor();
        final MessageProcessorID mprocID = this.entityToProcessor().getOrDefault(id, null);
        if (mprocID != null) {
            final MessageProcessor proc = idToProc.getOrDefault(mprocID, null);
            if (proc != null) {
                return proc;
            }
        }
        final MessageProcessorID dprocID = this.entityClassDefaults().getOrDefault(id.entityClass(), mprocID);
        final MessageProcessor dproc = idToProc.getOrDefault(dprocID, null);
        return dproc;
    }

    public abstract void registerProcessor(MessageProcessor processor);

    public abstract void registerProcessorDefault(MessageProcessor processor, Taggable.Tag entityClass);

    public abstract void registerEntity(final Entity entity, final MessageProcessorID processorID);

    public abstract void registerEntity(final IEntityID id, final MessageProcessorID processorID);

    public abstract MessageProcessingResult publish(final MessageContext context, final Event event);

    public abstract MessageProcessingResult send(final MessageContext context, final Command command);

    public abstract class AbstractMessageBus implements MessageBus {
        final private ConcurrentMap<IEntityID, MessageProcessorID> entityToProcessor = new ConcurrentHashMap<>();
        final private ConcurrentMap<Taggable.Tag, MessageProcessorID> entityClassDefaults = new ConcurrentHashMap<>();
        final private ConcurrentMap<MessageProcessorID, MessageProcessor> processorIDToProcessor = new ConcurrentHashMap<>();
        protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
        final private MessageProcessingResult.Failed NO_RESULT = MessageProcessingResult.Failed("no result");

        @Autowired
        private Duration timing;

        protected AbstractMessageBus(@Autowired Duration timing) {
            this.timing = timing;
            logger.atLevel(Level.TRACE).log("initialized");
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
        public ImmutableBiMap<Tag, MessageProcessorID> entityClassDefaults() {
            return ImmutableBiMap.copyOf(this.entityClassDefaults);
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
            logger.atTrace().addKeyValue("processorID", processorID).addKeyValue("entityID", id)
                    .log(() -> String.format("Processor for individual entity registered"));
        }

        @Override
        public MessageProcessingResult publish(MessageContext context, Event event) {
            if (event == null) {
                return MessageProcessingResult.Failed("cannot publish null event");
            }

            return this.handle(context, event);
        }

        @Override
        public MessageProcessingResult send(MessageContext context, Command command) {
            if (command == null) {
                return MessageProcessingResult.Failed("cannot send null command");
            }

            return this.handle(context, command);
        }

        protected abstract ExecutorService executor(String logname);

        @Override
        public MessageProcessor processorForEntity(IEntityID id) {
            if (id == null) {
                return null;
            }
            final MessageProcessorID mprocID = this.entityToProcessor.getOrDefault(id, null);
            if (mprocID != null) {
                final MessageProcessor proc = this.processorIDToProcessor.getOrDefault(mprocID, null);
                if (proc != null) {
                    return proc;
                }
            }
            final MessageProcessorID dprocID = this.entityClassDefaults.getOrDefault(id.entityClass(), mprocID);
            final MessageProcessor dproc = this.processorIDToProcessor.getOrDefault(dprocID, null);
            return dproc;
        }

        private MessageProcessingResult handle(final MessageContext context, final Message message) {
            if (message == null) {
                return MessageProcessingResult.Failed("cannot handle null message");
            }
            if (context == null) {
                return MessageProcessingResult.Failed("cannot direct message with null context");
            }

            final Map<String, String> preMDC = MDC.getCopyOfContextMap();

            MDC.pushByKey("messageType", message.getClass().getName());
            MDC.pushByKey("messageTsid", message.tsid().toString());
            if (this.logger.isDebugEnabled()) {
                MDC.pushByKey("messageContext", context.toString());
            }

            final Logger eventLogger = this.logger;
            final MessageProcessor processor = this.processorForEntity(context.destination());
            if (processor == null) {
                MessageProcessingResult.Failed toReturn = MessageProcessingResult.Failed("no processor found");
                this.logger.atWarn().addKeyValue("context.destination", context.destination()).log(toReturn.reason());
                MDC.setContextMap(preMDC);
                return toReturn;
            }

            MDC.pushByKey("processorId", processor.messageProcessorID().toString());
            final Map<String, String> asMap = MDC.getCopyOfContextMap();

            try {

                final ExecutorService executor = this.executor(String.format("%s.%s.%s", this.getClass().getName(),
                        message.getClass().getName(), message.tsid()));
                if (message instanceof Event asEvent) {
                    executor.submit(() -> {
                        MDC.setContextMap(asMap);
                        MessageProcessingResult myResult = NO_RESULT;
                        try {
                            eventLogger.trace("Started processing event");
                            myResult = processor.process(context, asEvent);
                        } finally {
                            eventLogger.trace(String.format("Processing event finished: %s", myResult));
                        }
                    });
                    MDC.setContextMap(preMDC);
                    return MessageProcessingResult.HANDLED;
                } else if (message instanceof Command asCommand) {
                    final MessageProcessingResult recieved = executor.submit(() -> {
                        MDC.setContextMap(asMap);
                        MessageProcessingResult value = NO_RESULT;
                        try {
                            eventLogger.trace("Starting processing command");
                            value = processor.process(context, asCommand);
                            return value;
                        } finally {
                            final String logMessage = String.format("Processing command finished: %s", value);
                            eventLogger.trace(logMessage);
                        }
                    }).get(timing.toNanos(), TimeUnit.NANOSECONDS);
                    MDC.setContextMap(preMDC);
                    return recieved;
                }
                final MessageProcessingResult recievedValue = executor.submit(() -> {
                    MDC.setContextMap(asMap);
                    MessageProcessingResult value = NO_RESULT;
                    try {
                        eventLogger.trace("Starting processing message");
                        value = processor.process(context, message);
                        return value;
                    } finally {
                        final String logMessage = String.format("Processing message finished: %s", value);
                        eventLogger.trace(logMessage);
                    }
                }).get(timing.toNanos(), TimeUnit.NANOSECONDS);
                MDC.setContextMap(preMDC);
                return recievedValue;
            } catch (NullPointerException | InterruptedException | ExecutionException | TimeoutException e) {
                eventLogger.warn("encountered exception", e);
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                eventLogger.warn("encountered runtime exception", e);
                throw e;
            }
            eventLogger.error("Should not have been able to reach here");
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
