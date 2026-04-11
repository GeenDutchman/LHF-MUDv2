package com.geendutchman.lhf_mudv2.execution;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessorID;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public sealed interface LHFCommand extends Message, Comparable<LHFCommand> {
    public static final TsidFactory idFactory = TsidFactory.newInstance1024(Math.abs("lhfcommands".hashCode() % 1024));

    @Override
    public default int compareTo(LHFCommand o) {
        return this.tsid().compareTo(o.tsid());
    }

    public record ReassignProcessor(Tsid tsid, IEntityID entity, MessageProcessorID processor) implements LHFCommand {
        public ReassignProcessor {
            Preconditions.checkNotNull(tsid, "tsid must not be null");
            Preconditions.checkNotNull(entity, "must reassign an entity");
            Preconditions.checkNotNull(processor, "must assign the entity a processor");
        }
    }

    public record LineCommand(Tsid tsid, String command, boolean autocomplete) implements LHFCommand {
        private final static Pattern splitter = Pattern.compile("\"((?:\"|[^\"])*?)\"|([^ ]+)");

        public LineCommand {
            Preconditions.checkNotNull(tsid, "tsid must not be null");
            Preconditions.checkNotNull(command, "command must not be null");
        }

        public String[] commandArray() {
            final Matcher splitten = splitter.matcher(command);
            final ArrayList<String> argsAL = new ArrayList<>();
            while (splitten.find()) {
                if (splitten.group(1) != null) {
                    argsAL.add(splitten.group(1));
                } else {
                    argsAL.add(splitten.group(2));
                }
            }
            final String[] args = argsAL.toArray(new String[0]);
            return args;
        }
    }

    public sealed interface BuilderFactoryCommand extends LHFCommand {
        public record CreateItemsForCreatureCommand(Tsid tsid, ItemBuilderFactory.LockedItemBuilder itemBuilder,
                CreatureID forCreature) implements BuilderFactoryCommand {
            public CreateItemsForCreatureCommand {
                Preconditions.checkNotNull(tsid, "tsid must not be null");
                Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
                Preconditions.checkNotNull(forCreature, "must target a creature");
            }
        }

        public record CreateItemsForRoomCommand(Tsid tsid, ItemBuilderFactory.LockedItemBuilder itemBuilder,
                RoomID forRoom) implements BuilderFactoryCommand {
            public CreateItemsForRoomCommand {
                Preconditions.checkNotNull(tsid, "tsid must not be null");
                Preconditions.checkNotNull(itemBuilder, "Itembuilder should not be null");
                Preconditions.checkNotNull(forRoom, "must target a room");
            }
        }

        public record CreateCreaturesForRoomCommand(Tsid tsid, CreatureBuilderFactory.Builder creatureBuilder,
                RoomID forRoom) implements BuilderFactoryCommand {
            public CreateCreaturesForRoomCommand {
                Preconditions.checkNotNull(tsid, "tsid must not be null");
                Preconditions.checkNotNull(creatureBuilder, "CreatureBuilder should not be null");
                Preconditions.checkNotNull(forRoom, "must target a room");
            }
        }

    }

    public sealed interface ChangeEntityCommand extends LHFCommand {
        public record ChangeCreatureCommand(Tsid tsid, ImmutableList<CreatureEffect> effects)
                implements ChangeEntityCommand {
            public ChangeCreatureCommand {
                Preconditions.checkNotNull(tsid, "tsid must not be null");
                Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            }
        }

        public record ChangeRoomCommand(Tsid tsid, ImmutableList<RoomEffect> effects) implements ChangeEntityCommand {
            public ChangeRoomCommand {
                Preconditions.checkNotNull(tsid, "tsid must not be null");
                Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            }
        }

        public record ChangeItemCommand(Tsid tsid, ImmutableList<ItemEffect> effects) implements ChangeEntityCommand {
            public ChangeItemCommand {
                Preconditions.checkNotNull(tsid, "tsid must not be null");
                Preconditions.checkNotNull(effects, "effects may be empty but must not be null");
            }
        }

    }

}
