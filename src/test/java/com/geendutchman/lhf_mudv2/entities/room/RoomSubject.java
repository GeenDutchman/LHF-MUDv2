package com.geendutchman.lhf_mudv2.entities.room;

import com.geendutchman.lhf_mudv2.entities.creature.CreatureContainerSubject;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainerSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

public final class RoomSubject extends Subject {
    public static Factory<RoomSubject, Room> rooms() {
        return RoomSubject::new;
    }

    public static RoomSubject assertThat(Room room) {
        return Truth.assertAbout(rooms()).that(room);
    }

    private final Room room;

    private RoomSubject(FailureMetadata metadata, Room room) {
        super(metadata, room);
        this.room = room;
    }

    public ItemContainerSubject items() {
        return check("items()").about(ItemContainerSubject.itemContainers()).that(this.room);
    }

    public CreatureContainerSubject creatures() {
        return check("creatures()").about(CreatureContainerSubject.creatureContainers()).that(this.room);
    }

    public OptionalSubject description() {
        return check("roomDescription()").that(this.room.description());
    }

    public StringSubject name() {
        return check("name()").that(this.room.name().toString());
    }

    public StringSubject roomID() {
        return check("roomID().tsid()").that(this.room.identifier().tsid().toString());
    }
}
