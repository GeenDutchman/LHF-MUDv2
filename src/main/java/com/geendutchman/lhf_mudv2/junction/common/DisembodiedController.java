package com.geendutchman.lhf_mudv2.junction.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.commandline.ACommandLineGenerator.CommandLineGenerator;
import com.google.common.base.Preconditions;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

@Component
public class DisembodiedController implements MessageProcessor {
    protected static final Examinable.Name NAME = new Examinable.Name("Disembodied Controller");

    protected final MessageBus bus;

    private final MessageProcessorID processorID;

    protected final Logger logger;

    protected final CommandLineGenerator base;

    protected DisembodiedController(@Autowired MessageBus bus, @Autowired CommandLineGenerator base) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        Preconditions.checkNotNull(base, "Command line generator should not be null");
        Examinable.Name name = this.name();
        if (name == null) {
            name = DisembodiedController.NAME;
        }
        this.processorID = new MessageProcessorID(name, MessageProcessor.messageProcessorTsidFactory.create());
        this.bus = bus;
        this.base = base;
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

    protected CommandLineGenerator getGenerator() {
        return this.base;
    }

    protected CommandLine generateCommandLine(final MessageContext context) {
        CommandLine generated = null;
        CommandLineGenerator effectiveGen = this.getGenerator();
        if (effectiveGen != null) {
            generated = effectiveGen.start(this.bus, context);
        }
        if (generated == null && this.base != null) {
            generated = this.base.start(this.bus, context);
        }
        return generated;
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
            case LHFCommand.ReassignProcessor rp ->
                MessageProcessingResult.Failed("cannot handle processor reassignment");
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
                CommandLine line = this.generateCommandLine(context);
                if (line == null) {
                    yield MessageProcessingResult.Failed("Could not produce a command line");
                }

                StringWriter sOut = new StringWriter();
                StringWriter sErr = new StringWriter();
                line.setOut(new PrintWriter(sOut)).setErr(new PrintWriter(sErr));

                int executionCode = line.execute(lc.commandArray());
                ParseResult parseResult = line.getParseResult();
                List<CommandLine> cmdlist = parseResult.asCommandLineList();
                logger.atTrace().addKeyValue("matched commands", cmdlist.stream()
                        .map(cl -> String.format("%s -> %s", cl.getCommandName(), cl.getExecutionResult())).toList())
                        .addKeyValue("args", lc.commandArray())
                        .log("executed command");

                RichOutput.Builder builder = null;
                String sOutString = sOut.toString();
                if (sOutString != null && sOutString.length() > 0) {
                    RichOutput sub = RichOutput.builder().setSequenceName("Command Line Output").addString(sOutString)
                            .build();
                    builder = RichOutput.builder().setSequenceName("Command Line").addOutput(sub);
                    this.logger.atTrace().addKeyValue("stdout", sOutString)
                            .log("Encountered command line stdout");
                }
                sOutString = sErr.toString();
                if (sOutString != null && sOutString.length() > 0) {
                    RichOutput sub = RichOutput.builder().setSequenceName("Command Line Error").addString(sOutString)
                            .build();
                    if (builder == null) {
                        builder = RichOutput.builder().setSequenceName("Command Line");
                    }
                    builder.addOutput(sub);
                    this.logger.atError().addKeyValue("stderr", sOutString)
                            .log("Encountered command line error");
                }

                if (builder != null) {
                    this.bus.publish(
                            MessageContext.builder().setDestination(context.sender()).setSender(context.sender())
                                    .build(),
                            Event.PlainEvent.asDescribed(builder.build()).plain());
                }

                Object result = cmdlist.getLast().getExecutionResult();
                if (result instanceof MessageProcessingResult mpr) {
                    yield mpr;
                } else {
                    yield MessageProcessingResult
                            .Failed(String.format("Execution code: %d, Unknown result -> '%s'", executionCode, result));
                }
            }

        };
    }

}
