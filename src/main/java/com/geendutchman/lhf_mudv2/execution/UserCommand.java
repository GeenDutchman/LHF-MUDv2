package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;

import com.google.common.base.Preconditions;

public sealed interface UserCommand extends Command {

    record SeeCommand(Optional<String> what) implements UserCommand {
        public SeeCommand {
            Preconditions.checkNotNull(what, "what to see may be empty, but not null");
            Preconditions.checkArgument(!what.orElse("null").isBlank(), "may not be populated by an empty string");
        }
    }

    record SayCommand(String message, Optional<String> toWhom) implements UserCommand {
        public SayCommand {
            Preconditions.checkNotNull(message, "message may not be null");
            Preconditions.checkArgument(!message.isBlank(), "message may not be blank");
            Preconditions.checkNotNull(toWhom, "to whom to speak may be empty, but not null");
        }
    }

    record TakeCommand(String what) implements UserCommand {
        public TakeCommand {
            Preconditions.checkNotNull(what, "object to take must not be null");
            Preconditions.checkArgument(!what.isBlank(), "item to take must not be blank");
        }

    }

    record DropCommand(String what) implements UserCommand {
        public DropCommand {
            Preconditions.checkNotNull(what, "what to drop must not be null");
            Preconditions.checkArgument(!what.isBlank(), "what to drop must not be blank");
        }
    }
}
