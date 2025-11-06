package com.geendutchman.lhf_mudv2.execution;

import java.util.UUID;

import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public sealed interface LHFCommand extends Command {

    public record CreateItemsForCreatureCommand(CommandRouting routing, UUID uuid,
            ItemBuilderFactory.LockedItemBuilder itemBuilder, CreatureID forCreature) implements LHFCommand {
        public CreateItemsForCreatureCommand {
            Command.commandPreconditions(routing, uuid);
            Preconditions.checkArgument(routing.destination().getPath().contains("/builderFactory/items"),
                    "should be directed to \"/builderFactory/items\" and not %s", routing.destination());
            Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
            Preconditions.checkNotNull(forCreature, "must target a creature");
        }
    }

    public record ChangeCreatureCommand(CommandRouting routing, UUID uuid, ImmutableList<CreatureEffect> effects)
            implements LHFCommand {
        public ChangeCreatureCommand {
            Command.commandPreconditions(routing, uuid);
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            Preconditions.checkArgument(routing.destination().getPath().contains("/creatures/"),
                    "must be targeted at a creature");
        }
    }

    public record CreateItemsForRoomCommand(CommandRouting routing, UUID uuid,
            ItemBuilderFactory.LockedItemBuilder itemBuilder, RoomID forRoom) implements LHFCommand {
        public CreateItemsForRoomCommand {
            Command.commandPreconditions(routing, uuid);
            Preconditions.checkArgument(routing.destination().getPath().contains("/builderFactory/items"),
                    "should be directed to \"/builderFactory/items\" and not %s", routing.destination());
            Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
            Preconditions.checkNotNull(forRoom, "must target a room");
        }
    }

    public record CreateCreaturesForRoomCommand(CommandRouting routing, UUID uuid,
            CreatureBuilderFactory.Builder creatureBuilder, RoomID forRoom) implements LHFCommand {
        public CreateCreaturesForRoomCommand {
            Command.commandPreconditions(routing, uuid);
            Preconditions.checkArgument(routing.destination().getPath().contains("/builderFactory/creatures"),
                    "should be directed to \"/builderFactory/creatures\" and not %s", routing.destination());
            Preconditions.checkNotNull(creatureBuilder, "CreatureBuilder should not be null");
            Preconditions.checkNotNull(forRoom, "must target a room");
        }
    }

    public record ChangeRoomCommand(CommandRouting routing, UUID uuid, ImmutableList<RoomEffect> effects)
            implements LHFCommand {
        public ChangeRoomCommand {
            Command.commandPreconditions(routing, uuid);
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            Preconditions.checkArgument(routing.destination().getPath().contains("/rooms/"),
                    "must be targeted at a room");
        }
    }

    public record ChangeItemCommand(CommandRouting routing, UUID uuid, ImmutableList<ItemEffect> effects)
            implements LHFCommand {
        public ChangeItemCommand {
            Command.commandPreconditions(routing, uuid);
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            Preconditions.checkArgument(routing.destination().getPath().contains("/items/"),
                    "must be targeted at an item");
        }
    }

}
