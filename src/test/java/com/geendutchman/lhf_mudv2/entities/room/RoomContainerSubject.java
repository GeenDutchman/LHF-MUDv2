package com.geendutchman.lhf_mudv2.entities.room;

import com.google.common.truth.CustomSubjectBuilder;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.MapSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

public class RoomContainerSubject<R extends Room> extends MapSubject {
    public static CustomSubjectBuilder.Factory<RoomContainerSubjectBuilder> roomContainers() {
        return RoomContainerSubjectBuilder::new;
    }

    public static <R extends Room> RoomContainerSubject<R> assertThat(RoomContainer<R> actual) {
        return Truth.assertAbout(roomContainers()).that(actual);
    }

    private final RoomContainer<R> actual;

    protected RoomContainerSubject(FailureMetadata metadata, RoomContainer<R> actual) {
        super(metadata, actual != null ? actual.rooms() : null);
        this.actual = actual;
    }

    public StringSubject name() {
        return check("name()").that(actual.name());
    }

    public MapSubject rooms() {
        return check("rooms()").that(actual.rooms());
    }

    public RoomContainerSubject<R> queryAll(RoomQuery query) {
        return check("queryAll(%s)", query).about(roomContainers()).that(this.actual.queryAll(query));
    }

    public OptionalSubject queryOne(RoomQuery query) {
        return check("queryOne(%s)", query).that(this.actual.queryOne(query));
    }

    public void hasRoom(Room room) {
        this.rooms().containsEntry(room.roomID(), room);
    }

}
