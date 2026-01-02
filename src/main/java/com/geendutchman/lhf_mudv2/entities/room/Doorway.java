package com.geendutchman.lhf_mudv2.entities.room;

import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.base.Preconditions;

public final record Doorway(RoomID target, CreatureQuery filter) implements Comparable<Doorway> {

    public Doorway {
        Preconditions.checkNotNull(target, "target room must not be null");
        Preconditions.checkNotNull(filter, "filter may have no criteria, but must not be null");
    }

    @Override
    public int compareTo(Doorway o) {
        if (this.equals(o)) {
            return 0;
        }

        return this.target.compareTo(o.target);
    }

}