package com.geendutchman.lhf_mudv2.events;

import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

@AutoValue
public abstract class Event {
    public static Event describePlainEvent(RichOutput.Builder richOutputBuilder) {
        Preconditions.checkArgument(richOutputBuilder != null, "description builder must not be null");
        return new AutoValue_Event(Optional.of(richOutputBuilder.build()));
    }

    private final UUID uuid = UUID.randomUUID();

    public final UUID uuid() {
        return this.uuid;
    }

    public abstract Optional<RichOutput> description();

}
