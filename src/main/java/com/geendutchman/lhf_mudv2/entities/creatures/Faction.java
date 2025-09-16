package com.geendutchman.lhf_mudv2.entities.creatures;

import com.geendutchman.lhf_mudv2.display.Taggable;

// Describes what faction a creature belongs to
public enum Faction implements Taggable {
    // Renegades are every creature for themselves, they have turned their back on
    // their own faction, and thus, have no friends. There are no enforced penalties
    // for killing a renegade, even for other renegades.
    RENEGADE,
    // A player is typically controlled by a human being on a device, or ahem, is
    // sent by the gods to *do things* and *solve problems*.
    PLAYER,
    // An NPC is a child or decendant of the gods, born and raised here.
    NPC,
    // A pet is friendly to most
    PET,
    // Monsters are spawned by the environment, a danger to life here.
    MONSTER,
    // The swarm is out to destroy everyone else who is not of the swarm.
    SWARM;

    public static final Taggable.Tag FACTION_TAG = new Taggable.Tag("FACTION");

    @Override
    public Tag tag() {
        return FACTION_TAG;
    }

    @Override
    public String content() {
        return this.name();
    }
}
