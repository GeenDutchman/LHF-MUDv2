package com.geendutchman.lhf_mudv2.execution.commandline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.commandline.CommandHandler.PingCommandHandler;
import com.google.common.base.Preconditions;

@Component(value = "commandline")
@Scope("prototype")
public class CreatureCommandLineGenerator extends ACommandLineGenerator {

    @Autowired
    protected final CommandLineGenerator base;
    protected final ItemRepository itemRepository;
    protected final CreatureRepository creatureRepository;
    protected final RoomRepository roomRepository;

    public CreatureCommandLineGenerator(@Autowired CommandLineGenerator base, ItemRepository itemRepository,
            CreatureRepository creatureRepository, RoomRepository roomRepository) {
        Preconditions.checkNotNull(base, "base command line generator should not be null");
        this.base = base;
        this.itemRepository = itemRepository;
        this.creatureRepository = creatureRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public CommandLineGenerator base() {
        return this.base;
    }

    @Override
    public MudCommandLine start(final MessageBus bus, final MessageContext t) {

        final MudCommandLine line = this.base.start(bus, t);
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
            line.registerConverter(Creature.class, line.getInjectedFactory().getCfc());
            line.registerConverter(Item.class, line.getInjectedFactory().getIfc());
            line.registerConverter(Entity.class, line.getInjectedFactory().getEfc());
            line.registerConverter(IEntityID.class, line.getInjectedFactory().getEic());
        }
        return line;
    }

}
