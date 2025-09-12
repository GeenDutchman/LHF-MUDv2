package com.geendutchman.lhf_mudv2.entities.room;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.MapSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StreamSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

public class RoomContainerSubject extends MapSubject {
    public static Factory<RoomContainerSubject, RoomContainer> roomContainers() {
        return RoomContainerSubject::new;
    }

    public static RoomContainerSubject assertThat(RoomContainer actual) {
        return Truth.assertAbout(roomContainers()).that(actual);
    }

    private final RoomContainer actual;

    protected RoomContainerSubject(FailureMetadata metadata, RoomContainer actual) {
        super(metadata, actual != null ? actual.roomMap() : null);
        this.actual = actual;
    }

    public StringSubject name() {
        return check("name()").that(actual.name().toString());
    }

    public StreamSubject rooms() {
        return check("rooms()").that(actual.rooms());
    }

    public MapSubject roomMap() {
        return check("rooms()").that(actual.roomMap());
    }

    public RoomContainerSubject queryAll(RoomQuery query) {
        return check("queryAll(%s)", query).about(roomContainers()).that(this.actual.queryRooms(query));
    }

    public OptionalSubject queryOne(RoomQuery query) {
        return check("queryOne(%s)", query).that(this.actual.queryOneRoom(query));
    }

    public void hasRoom(Room room) {
        this.roomMap().containsEntry(room.roomID(), room);
    }

}
