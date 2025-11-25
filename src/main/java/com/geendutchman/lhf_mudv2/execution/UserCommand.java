package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;

public sealed interface UserCommand extends Command {
    public static final TsidFactory idFactory = TsidFactory.newInstance1024("usercommands".hashCode() % 1024);

    record SeeCommand(Tsid tsid, Optional<String> what) implements UserCommand {
        public SeeCommand {
            Preconditions.checkNotNull(tsid, "tsid must not be null");
            Preconditions.checkNotNull(what, "what to see may be empty, but not null");
            Preconditions.checkArgument(!what.orElse("null").isBlank(), "may not be populated by an empty string");
        }
    }

    record SayCommand(Tsid tsid, String message, Optional<String> toWhom) implements UserCommand {
        public SayCommand {
            Preconditions.checkNotNull(tsid, "tsid must not be null");
            Preconditions.checkNotNull(message, "message may not be null");
            Preconditions.checkArgument(!message.isBlank(), "message may not be blank");
            Preconditions.checkNotNull(toWhom, "to whom to speak may be empty, but not null");
        }
    }

    record TakeCommand(Tsid tsid, String what) implements UserCommand {
        public TakeCommand {
            Preconditions.checkNotNull(tsid, "tsid must not be null");
            Preconditions.checkNotNull(what, "object to take must not be null");
            Preconditions.checkArgument(!what.isBlank(), "item to take must not be blank");
        }

    }

    record DropCommand(Tsid tsid, String what) implements UserCommand {
        public DropCommand {
            Preconditions.checkNotNull(tsid, "tsid must not be null");
            Preconditions.checkNotNull(what, "what to drop must not be null");
            Preconditions.checkArgument(!what.isBlank(), "what to drop must not be blank");
        }
    }
}
