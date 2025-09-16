package com.geendutchman.lhf_mudv2.entities.creatures;

import com.geendutchman.lhf_mudv2.display.Taggable;

public enum CreatureStats implements Taggable {
    HEALTH, MAX_HEALTH, EXPERIENCE, XP_THRESHOLD;

    public static final Taggable.Tag STAT_TAG = new Taggable.Tag("STAT");

    @Override
    public Tag tag() {
        return STAT_TAG;
    }

    @Override
    public String content() {
        return this.name();
    }
}
