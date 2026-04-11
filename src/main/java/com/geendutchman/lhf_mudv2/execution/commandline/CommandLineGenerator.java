package com.geendutchman.lhf_mudv2.execution.commandline;

import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.commandline.CommandHandler.PingCommandHandler;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IFactory;

@Component(value = "commandline")
public record CommandLineGenerator(IFactory factory, ObjectProvider<ExitHandler> exitHandler,
        ObjectProvider<PingCommandHandler> pingHandler, ObjectProvider<DropCommand> dropCommand,
        ObjectProvider<GoCommand> goCommand, ObjectProvider<InventoryCommand> inventoryCommand,
        ObjectProvider<SayCommand> sayCommand, ObjectProvider<SeeCommand> seeCommand,
        ObjectProvider<StatusCommand> statusCommand, ObjectProvider<TakeCommand> takeCommand)
        implements Function<MessageContext, CommandLine> {

    @Command(name = "mud", description = "The base of all commands to do things here.", subcommands = {
            HelpCommand.class })
    private static record MudCommand() {
    }

    @Override
    public CommandLine apply(MessageContext t) {
        IFactory injectFactory = new IFactory() {

            @Override
            public <K> K create(Class<K> cls) throws Exception {
                try {
                    if (t.getClass().isAssignableFrom(cls)) {
                        @SuppressWarnings("unchecked")
                        K result = (K) t;
                        return result;
                    }
                    return factory.create(cls);
                } catch (Exception e) {
                    return CommandLine.defaultFactory().create(cls);
                }
            }

        };
        final CommandLine line = new CommandLine(new MudCommand(), injectFactory);
        pingHandler.ifAvailable(ping -> line.addSubcommand(ping));
        exitHandler.ifAvailable(exit -> line.addSubcommand(exit));
        dropCommand.ifAvailable(drop -> line.addSubcommand(drop));
        goCommand.ifAvailable(go -> line.addSubcommand(go));
        inventoryCommand.ifAvailable(inv -> line.addSubcommand(inv));
        sayCommand.ifAvailable(say -> line.addSubcommand(say));
        seeCommand.ifAvailable(see -> line.addSubcommand(see));
        statusCommand.ifAvailable(status -> line.addSubcommand(status));
        takeCommand.ifAvailable(take -> line.addSubcommand(take));
        return line;
    }

}
