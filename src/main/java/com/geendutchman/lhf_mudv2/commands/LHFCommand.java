package com.geendutchman.lhf_mudv2.commands;

import java.util.UUID;

import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public sealed interface LHFCommand extends Command {
    public enum LHFCommandType {
        CREATE_ITEMS_FOR_CREATURE, CHANGE_CREATURE, CREATE_ITEMS_FOR_ROOM, CREATE_CREATURE_FOR_ROOM, CHANGE_ROOM;
    }

    static void lhfCommandPreconditions(LHFCommandType expected, LHFCommandType commandType, MessageRouting routing,
            UUID uuid) {
        Preconditions.checkArgument(commandType == expected, "this should only be a %s", expected.name());
        Command.commandPreconditions(routing, uuid);
    }

    public abstract LHFCommandType commandType();

    public record CreateItemsForCreatureCommand(LHFCommandType commandType, MessageRouting routing, UUID uuid,
            ItemBuilderFactory.LockedItemBuilder itemBuilder, CreatureID forCreature) implements LHFCommand {
        public CreateItemsForCreatureCommand {
            lhfCommandPreconditions(commandType, LHFCommandType.CREATE_ITEMS_FOR_CREATURE, routing, uuid);
            Preconditions.checkArgument(routing.destination().getPath().contains("/builderFactory/items"),
                    "should be directed to \"/builderFactory/items\" and not %s", routing.destination());
            Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
            Preconditions.checkNotNull(forCreature, "must target a creature");
        }
    }

    public record ChangeCreatureCommand(LHFCommandType commandType, MessageRouting routing, UUID uuid,
            ImmutableList<CreatureEffect> effects) implements LHFCommand {
        public ChangeCreatureCommand {
            lhfCommandPreconditions(commandType, LHFCommandType.CHANGE_CREATURE, routing, uuid);
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            Preconditions.checkArgument(routing.destination().getPath().contains("/creatures/"),
                    "must be targeted at a creature");
        }
    }

    public record CreateItemsForRoomCommand(LHFCommandType commandType, MessageRouting routing, UUID uuid,
            ItemBuilderFactory.LockedItemBuilder itemBuilder, RoomID forRoom) implements LHFCommand {
        public CreateItemsForRoomCommand {
            lhfCommandPreconditions(commandType, LHFCommandType.CREATE_ITEMS_FOR_ROOM, routing, uuid);
            Preconditions.checkArgument(routing.destination().getPath().contains("/builderFactory/items"),
                    "should be directed to \"/builderFactory/items\" and not %s", routing.destination());
            Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
            Preconditions.checkNotNull(forRoom, "must target a room");
        }
    }

    public record CreateCreaturesForRoomCommand(LHFCommandType commandType, MessageRouting routing, UUID uuid,
            CreatureBuilderFactory.Builder creatureBuilder, RoomID forRoom) implements LHFCommand {
        public CreateCreaturesForRoomCommand {
            lhfCommandPreconditions(LHFCommandType.CREATE_CREATURE_FOR_ROOM, commandType, routing, uuid);
            Preconditions.checkArgument(routing.destination().getPath().contains("/builderFactory/creatures"),
                    "should be directed to \"/builderFactory/creatures\" and not %s", routing.destination());
            Preconditions.checkNotNull(creatureBuilder, "CreatureBuilder should not be null");
            Preconditions.checkNotNull(forRoom, "must target a room");
        }
    }

    public record ChangeRoomCommand(LHFCommandType commandType, MessageRouting routing, UUID uuid,
            ImmutableList<RoomEffect> effects) implements LHFCommand {
        public ChangeRoomCommand {
            lhfCommandPreconditions(commandType, commandType, routing, uuid);
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            Preconditions.checkArgument(routing.destination().getPath().contains("/rooms/"),
                    "must be targeted at a room");
        }
    }

}
