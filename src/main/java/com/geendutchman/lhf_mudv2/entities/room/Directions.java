package com.geendutchman.lhf_mudv2.entities.room;

import com.geendutchman.lhf_mudv2.display.Taggable;

public enum Directions implements Taggable {
    NORTH, EAST, SOUTH, WEST, UP, DOWN;

    public Directions opposite() {
        switch (this) {
        case DOWN:
            return UP;
        case EAST:
            return WEST;
        case NORTH:
            return SOUTH;
        case SOUTH:
            return NORTH;
        case UP:
            return DOWN;
        case WEST:
            return EAST;
        default:
            return SOUTH;

        }
    }

    public static Directions insensitiveValueOf(String name) {
        if (name == null || name.isBlank()) {
            return Directions.valueOf(name);
        }
        final String treated = name.trim().toUpperCase();
        for (Directions dir : Directions.values()) {
            if (dir.name().startsWith(treated)) {
                return dir;
            }
        }
        return Directions.valueOf(treated);
    }

    public final static Tag DIRECTION_TAG = new Tag("DIRECTION");

    @Override
    public Tag tag() {
        return DIRECTION_TAG;
    }

    @Override
    public String content() {
        return this.name();
    }
}
