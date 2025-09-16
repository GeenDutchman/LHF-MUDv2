package com.geendutchman.lhf_mudv2.entities.creatures;

import com.geendutchman.lhf_mudv2.display.Taggable;

// Attributes of a Creature
public enum AttributeScores implements Taggable {
    // Strength of body
    FORCE,
    /// Flexibility of body
    FINESSE,
    // Knowing about
    SMARTS,
    // Knowing how
    SAVVY,
    // Can take a physical hit
    RUGGED,
    // Can take a mental/emotional hit
    RESOLVE;

    public static final Taggable.Tag ATTRIBUTE_TAG = new Taggable.Tag("ATTRIBUTE");

    @Override
    public Tag tag() {
        return ATTRIBUTE_TAG;
    }

    @Override
    public String content() {
        return this.name();
    }

    public static byte calulateModifier(int score) {
        int asint = (score - 10) / 2;
        if (asint > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        } else if (asint < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        }
        return (byte) asint;
    }

    public static byte calulateModifer(byte score) {
        if (score > Byte.MIN_VALUE + 10) {
            score = Byte.MIN_VALUE + 10;
        }
        return (byte) ((score - 10) / 2);
    }
}
