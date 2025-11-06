package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Preconditions;

public sealed interface UserCommand extends Command {
    public enum UserCommandType {
        SEE, SAY, TAKE, DROP;
    }

    public abstract UserCommandType commandType();

    static void userCommandPreconditions(UserCommandType expected, UserCommandType commandType, CommandRouting routing,
            UUID uuid) {
        Preconditions.checkArgument(commandType == expected, "this should only be a %s", expected.name());
        Command.commandPreconditions(routing, uuid);
    }

    record SeeCommand(UserCommandType commandType, CommandRouting routing, UUID uuid, Optional<String> what)
            implements UserCommand {
        public SeeCommand {
            userCommandPreconditions(UserCommandType.SEE, commandType, routing, uuid);
            Preconditions.checkNotNull(what, "what to see may be empty, but not null");
        }
    }

    record SayCommand(UserCommandType commandType, CommandRouting routing, UUID uuid, String message,
            Optional<String> toWhom) implements UserCommand {
        public SayCommand {
            userCommandPreconditions(UserCommandType.SAY, commandType, routing, uuid);
            Preconditions.checkNotNull(message, "message may not be null");
            Preconditions.checkArgument(!message.isBlank(), "message may not be blank");
            Preconditions.checkNotNull(toWhom, "to whom to speak may be empty, but not null");
        }
    }

    record TakeCommand(UserCommandType commandType, CommandRouting routing, UUID uuid, String what)
            implements UserCommand {
        public TakeCommand {
            userCommandPreconditions(UserCommandType.TAKE, commandType, routing, uuid);
            Preconditions.checkNotNull(what, "object to take must not be null");
            Preconditions.checkArgument(!what.isBlank(), "item to take must not be blank");
        }

    }

    record DropCommand(UserCommandType commandType, CommandRouting routing, UUID uuid, String what)
            implements UserCommand {
        public DropCommand {
            userCommandPreconditions(UserCommandType.DROP, commandType, routing, uuid);
            Preconditions.checkNotNull(what, "what to drop must not be null");
            Preconditions.checkArgument(!what.isBlank(), "what to drop must not be blank");

        }
    }
}
