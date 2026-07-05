package com.geendutchman.lhf_mudv2.junction.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.google.common.base.Preconditions;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

@Component
public class DisembodiedController implements MessageProcessor {
    protected static final Examinable.Name NAME = new Examinable.Name("Disembodied Controller");

    @Autowired
    protected final MessageBus bus;

    private final MessageProcessorID processorID;

    protected final Logger logger;

    protected DisembodiedController(@Autowired MessageBus bus) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        Examinable.Name name = this.name();
        if (name == null) {
            name = DisembodiedController.NAME;
        }
        this.processorID = new MessageProcessorID(name, MessageProcessor.messageProcessorTsidFactory.create());
        this.bus = bus;
        this.logger = LoggerFactory.getLogger(String.format("%s.%s", this.getClass().getName(), name));

    }

    @PostConstruct
    public void register() {
        if (this.getClass() == DisembodiedController.class) {
            this.bus.registerProcessorDefault(this, DisembodiedEntity.ENTITY_CLASS_DISEMBODIED);
        } else {
            this.bus.registerProcessor(this);
        }
    }

    protected Examinable.Name name() {
        return DisembodiedController.NAME;
    }

    @Override
    public MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    protected CommandLine generateCommandLine(final MessageBus bus, final MessageContext context) {
        // TODO: make this up
        throw new UnsupportedOperationException("Unimplemented method 'generateCommandLine'");
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public final MessageProcessingResult process(MessageContext context, Message message) {
        if (message == null) {
            return MessageProcessingResult.Failed("cannot handle null message");
        }
        return switch (message) {
        case LHFCommand c -> this.process(context, c);
        case Event e -> this.process(context, e);
        default -> MessageProcessingResult.Failed("unknown message type");
        };
    }

    @Override
    public final MessageProcessingResult process(MessageContext context, LHFCommand lhfCommand) {
        if (lhfCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null lhf command");
        }

        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult.Failed("cannot handle processor reassignment");
        case LHFCommand.BuilderFactoryCommand bfc -> MessageProcessingResult
                .Failed("disembodied entity cannot build anything");
        case LHFCommand.ChangeEntityCommand cec -> {
            yield switch (cec) {
            case LHFCommand.ChangeEntityCommand.ChangeItemCommand cic -> MessageProcessingResult
                    .Failed("disembodied entity cannot change items");
            case LHFCommand.ChangeEntityCommand.ChangeRoomCommand crc -> MessageProcessingResult
                    .Failed("disembodied entity cannot change room");
            case LHFCommand.ChangeEntityCommand.ChangeCreatureCommand ccc -> MessageProcessingResult
                    .Failed("disembodied entity cannot change creature");
            };
        }
        case LHFCommand.LineCommand lc -> {
            CommandLine line = this.generateCommandLine(bus, context);
            if (line == null) {
                yield MessageProcessingResult.Failed("Could not produce a command line");
            }
            line.execute(lc.commandArray());
            ParseResult parseResult = line.getParseResult();
            List<CommandLine> cmdlist = parseResult.asCommandLineList();
            Object result = cmdlist.getLast().getExecutionResult();
            if (result instanceof MessageProcessingResult mpr) {
                yield mpr;
            } else {
                yield MessageProcessingResult.Failed(String.format("Unknown result -> '%s'", result));
            }
        }

        };
    }

}
