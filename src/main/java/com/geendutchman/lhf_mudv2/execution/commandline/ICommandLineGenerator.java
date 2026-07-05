package com.geendutchman.lhf_mudv2.execution.commandline;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.LogWriter;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.commandline.CommandHandler.PingCommandHandler;
import com.google.common.base.Preconditions;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IFactory;

public abstract class ICommandLineGenerator {

    @Command(name = "mud", description = "The base of all commands to do things here.", subcommands = {
            HelpCommand.class })
    private static record MudCommand() implements Callable<MessageProcessingResult> {

        @Override
        public MessageProcessingResult call() throws Exception {
            throw new UnsupportedOperationException("You must specify a subcommand");
        }
    }

    /**
     * Just a small class for bundling purposes
     */
    public static class MudCommandLine extends CommandLine {
        private final InjectedFactory injectedFactory;
        private final MessageContext context;

        private static MudCommandLine create(Object command, IFactory factory, MessageContext ctx) {
            final InjectedFactory injectFactory = new InjectedFactory(factory, ctx);
            return new MudCommandLine(command, injectFactory, ctx);
        }

        private MudCommandLine(Object command, InjectedFactory factory, MessageContext ctx) {
            super(command, factory);
            this.injectedFactory = factory;
            this.context = ctx;
        }

        public InjectedFactory getInjectedFactory() {
            return injectedFactory;
        }

        public MessageContext getContext() {
            return context;
        }

    }

    @Component
    @Scope("singleton")
    public final class CommandLineGenerator extends ICommandLineGenerator {
        @Autowired
        private final IFactory factory;

        protected CommandLineGenerator(@Autowired IFactory fact) {
            Preconditions.checkNotNull(fact, "factory must not be null");
            this.factory = fact;
        }

        private MudCommandLine trueStart(final MessageBus bus, final MessageContext t) {
            final MudCommandLine line = MudCommandLine.create(new MudCommand(), this.factory(), t);
            final PingCommandHandler ping = new PingCommandHandler(bus, t);
            line.addSubcommand(ping);
            final Logger logger = LoggerFactory.getLogger(MudCommandLine.class);
            line.setOut(new PrintWriter(new LogWriter(logger)))
                    .setErr(new PrintWriter(new LogWriter(logger, Level.ERROR)));
            return line;
        }

        @Override
        public CommandLineGenerator base() {
            return this;
        }

        private IFactory trueFactory() {
            return this.factory;
        }

    }

    public final IFactory factory() {
        return this.base().trueFactory();
    }

    public abstract CommandLineGenerator base();

    public MudCommandLine start(final MessageBus bus, final MessageContext t) {
        return this.base().trueStart(bus, t);
    }
}
