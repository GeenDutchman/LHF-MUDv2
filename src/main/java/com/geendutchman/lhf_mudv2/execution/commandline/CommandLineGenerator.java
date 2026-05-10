package com.geendutchman.lhf_mudv2.execution.commandline;

import java.util.function.BiFunction;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.commandline.CommandHandler.PingCommandHandler;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.CreatureFromContext;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.EntityFromContext;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.ItemFromContext;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IFactory;

@Component(value = "commandline")
@Scope("prototype")
public record CommandLineGenerator(IFactory factory, ItemRepository itemRepository,
        CreatureRepository creatureRepository, RoomRepository roomRepository, ObjectProvider<ExitHandler> exitHandler,
        ObjectProvider<PingCommandHandler> pingHandler, ObjectProvider<DropCommand> dropCommand,
        ObjectProvider<GoCommand> goCommand, ObjectProvider<InventoryCommand> inventoryCommand,
        ObjectProvider<SayCommand> sayCommand, ObjectProvider<SeeCommand> seeCommand,
        ObjectProvider<StatusCommand> statusCommand, ObjectProvider<TakeCommand> takeCommand)
        implements BiFunction<MessageBus, MessageContext, CommandLine> {

    @Command(name = "mud", description = "The base of all commands to do things here.", subcommands = {
            HelpCommand.class })
    private static record MudCommand() {
    }

    @Override
    public CommandLine apply(final MessageBus bus, final MessageContext t) {
        IFactory injectFactory = new IFactory() {

            @Override
            public <K> K create(Class<K> cls) throws Exception {
                try {
                    if (t != null && t.getClass().isAssignableFrom(cls)) {
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
        PingCommandHandler ping = new PingCommandHandler(bus, t);
        line.addSubcommand(ping);
        ExitHandler exit = new ExitHandler(bus, t, this.itemRepository, this.creatureRepository, this.roomRepository);
        line.addSubcommand(exit);
        DropCommand drop = new DropCommand(bus, t);
        line.addSubcommand(drop);
        GoCommand go = new GoCommand(bus, t, this.roomRepository);
        line.addSubcommand(go);
        InventoryCommand inventory = new InventoryCommand(bus, t);
        line.addSubcommand(inventory);
        SayCommand say = new SayCommand(bus, t);
        line.addSubcommand(say);
        SeeCommand see = new SeeCommand(bus, t);
        line.addSubcommand(see);
        StatusCommand status = new StatusCommand(bus, t);
        line.addSubcommand(status);
        TakeCommand take = new TakeCommand(bus, t);
        line.addSubcommand(take);

        if (t != null) {
            line.registerConverter(Creature.class, new CreatureFromContext(t));
            line.registerConverter(Item.class, new ItemFromContext(t));
            line.registerConverter(Entity.class, new EntityFromContext(t));
        }
        return line;
    }

}
