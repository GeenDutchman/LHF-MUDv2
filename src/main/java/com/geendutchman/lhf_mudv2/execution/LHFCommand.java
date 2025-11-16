package com.geendutchman.lhf_mudv2.execution;

import java.util.UUID;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessorID;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public sealed interface LHFCommand extends Command {

    public record ReassignProcessor(UUID uuid, IEntityID entity, MessageProcessorID processor) implements LHFCommand {
        public ReassignProcessor {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkNotNull(entity, "must reassign an entity");
            Preconditions.checkNotNull(processor, "must assign the entity a processor");
        }
    }

    public sealed interface BuilderFactoryCommand extends LHFCommand {
        public record CreateItemsForCreatureCommand(UUID uuid, ItemBuilderFactory.LockedItemBuilder itemBuilder,
                CreatureID forCreature) implements BuilderFactoryCommand {
            public CreateItemsForCreatureCommand {
                Preconditions.checkNotNull(uuid, "uuid must not be null");
                Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
                Preconditions.checkNotNull(forCreature, "must target a creature");
            }
        }

        public record CreateItemsForRoomCommand(UUID uuid, ItemBuilderFactory.LockedItemBuilder itemBuilder,
                RoomID forRoom) implements BuilderFactoryCommand {
            public CreateItemsForRoomCommand {
                Preconditions.checkNotNull(uuid, "uuid must not be null");
                Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
                Preconditions.checkNotNull(forRoom, "must target a room");
            }
        }

        public record CreateCreaturesForRoomCommand(UUID uuid, CreatureBuilderFactory.Builder creatureBuilder,
                RoomID forRoom) implements BuilderFactoryCommand {
            public CreateCreaturesForRoomCommand {
                Preconditions.checkNotNull(uuid, "uuid must not be null");
                Preconditions.checkNotNull(creatureBuilder, "CreatureBuilder should not be null");
                Preconditions.checkNotNull(forRoom, "must target a room");
            }
        }

    }

    public record ChangeCreatureCommand(UUID uuid, ImmutableList<CreatureEffect> effects) implements LHFCommand {
        public ChangeCreatureCommand {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
        }
    }

    public record ChangeRoomCommand(UUID uuid, ImmutableList<RoomEffect> effects) implements LHFCommand {
        public ChangeRoomCommand {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
        }
    }

    public record ChangeItemCommand(UUID uuid, ImmutableList<ItemEffect> effects) implements LHFCommand {
        public ChangeItemCommand {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
        }
    }

}
