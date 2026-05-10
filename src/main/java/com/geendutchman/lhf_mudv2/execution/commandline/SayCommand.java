package com.geendutchman.lhf_mudv2.execution.commandline;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableList;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

@Component
@Scope("prototype")
@Command(name = "say", description = "Speak a message", subcommands = { HelpCommand.class })
public final class SayCommand extends UserCommandHandler {

    public SayCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Parameters(arity = "1..*", index = "*", description = "What message do you want to send? Surround the whole message \"In quotes\" for the most predictable handling.")
    protected ImmutableList<String> message;

    @Override
    public void run() {
        if (context.sender().room().isPresent()) {
            RichOutput.Builder builder = RichOutput.builder();
            for (final String string : message) {
                builder.addString(string);
            }

            this.bus.publish(
                    MessageContext.builder().setSender(context.sender())
                            .setDestinationId(context.sender().room().get().roomID()).build(),
                    Event.SpokenEvent.speaking(context.sender().baseId(), builder.build()));
            return;
        } else {
            // TODO: alert about problem
        }
    }

    @Command(name = "to", description = "Directs the message to a particular creature. Provide the name \"In quotes\" for the best results.", subcommands = {
            HelpCommand.class })
    public void to(
            @Parameters(arity = "1", index = "0", description = "To whom this message is directed.") Creature listener) {
        final RichOutput.Builder builder = RichOutput.builder();
        for (final String string : message) {
            builder.addString(string);
        }
        this.bus.publish(
                MessageContext.builder().setSender(context.sender()).setDestinationId(listener.creatureID()).build(),
                Event.SpokenEvent.speaking(context.sender().baseId(), builder.build()));
    }

}
