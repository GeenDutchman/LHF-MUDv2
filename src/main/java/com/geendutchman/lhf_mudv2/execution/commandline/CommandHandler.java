package com.geendutchman.lhf_mudv2.execution.commandline;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageBus;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class CommandHandler implements Runnable {
    @Spec
    protected CommandSpec spec;

    @Autowired
    protected final MessageBus bus;

    protected final MessageContext context;

    @Autowired
    protected CommandHandler(MessageBus messbus, MessageContext context) {
        this.bus = messbus;
        this.context = context;
    }

    @Spec
    public final void setSpec(CommandSpec spec) {
        this.spec = spec;
    }

    public final Appendable appendError(CharSequence seq) {
        try {
            final CommandLine cl = this.spec.commandLine();
            final PrintWriter err = cl.getErr();
            if (seq != null) {
                err.append(seq);
            }
            return err;
        } catch (NullPointerException e) {
            final Logger logger = LoggerFactory.getLogger(this.getClass());
            return new Appendable() {

                @Override
                public Appendable append(CharSequence csq) throws IOException {
                    if (csq != null) {
                        logger.atError().addKeyValue("context", context).log(csq.toString());
                    }
                    return this;
                }

                @Override
                public Appendable append(CharSequence csq, int start, int end) throws IOException {
                    if (csq != null) {
                        logger.atError().addKeyValue("context", context).log(csq.subSequence(start, end).toString());
                    }
                    return this;
                }

                @Override
                public Appendable append(char c) throws IOException {
                    logger.atError().addKeyValue("context", context).log("Single character: %s", c);
                    return this;
                }

            };
        }
    }

    public final Appendable appendOut(CharSequence seq) {
        try {
            final CommandLine cl = this.spec.commandLine();
            final PrintWriter err = cl.getOut();
            err.append(seq);
            return err;
        } catch (NullPointerException e) {
            final Logger logger = LoggerFactory.getLogger(this.getClass());
            return new Appendable() {

                @Override
                public Appendable append(CharSequence csq) throws IOException {
                    if (csq != null) {
                        logger.atInfo().addKeyValue("context", context).log(csq.toString());
                    }
                    return this;
                }

                @Override
                public Appendable append(CharSequence csq, int start, int end) throws IOException {
                    if (csq != null) {
                        logger.atInfo().addKeyValue("context", context).log(csq.subSequence(start, end).toString());
                    }
                    return this;
                }

                @Override
                public Appendable append(char c) throws IOException {
                    logger.atInfo().addKeyValue("context", context).log("Single character: %s", c);
                    return this;
                }

            };
        }
    }

    protected static final class Defer implements AutoCloseable {
        private final ArrayDeque<Runnable> onClose = new ArrayDeque<>();

        public Defer() {
        }

        public void addLast(Runnable r) {
            if (r != null) {
                onClose.addLast(r);
            }
        }

        @Override
        public void close() {
            for (final Runnable r : onClose) {
                r.run();
            }
        }
    }

    @Component
    @Scope("prototype")
    @Command(name = "ping", description = "Tests connectivity", subcommands = { HelpCommand.class })
    public final class PingCommandHandler extends CommandHandler {

        @Autowired
        protected PingCommandHandler(MessageBus messbus, MessageContext context) {
            super(messbus, context);
        }

        @Override
        public void run() {
            this.spec.commandLine().getOut().println("pong");
        }

    }

}
