package com.geendutchman.lhf_mudv2.dice;

import java.util.Map.Entry;
import java.util.function.UnaryOperator;

import com.geendutchman.lhf_mudv2.dice.DiceSet.DiceSetBuilder;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

/**
 * This represents different things one can do to modify a {@link DiceSet}
 */
public sealed interface DiceMods<E extends Enum<E>> extends UnaryOperator<DiceSet<E>>, Taggable
        permits DiceMods.FlavoredBonus, DiceMods.DoubleDice, DiceMods.MoreDice {

    /**
     * A tag for dice mods
     * 
     * @see Taggable
     */
    final static Taggable.Tag DICE_MOD_TAG = new Taggable.Tag("DICE_MODIFIER");

    @Override
    public default Taggable.Tag tag() {
        return DICE_MOD_TAG;
    }

    /**
     * This is a small bonus to one particular flavor, like "slashing + 1"
     */
    public record FlavoredBonus<E extends Enum<E>>(E flavor, byte bonus) implements DiceMods<E> {

        /**
         * This creates a Flavored Bonus as a Dice Mod
         * 
         * @param flavor
         * @param bonus
         */
        public FlavoredBonus {
            Preconditions.checkNotNull(flavor, "bonus flavor cannot be null");
        }

        @Override
        public DiceSet<E> apply(DiceSet<E> t) {
            return t.toBuilder().addBonus(flavor, bonus).addNote(String.format("(added bonus %d)", bonus)).build();
        }

        @Override
        public String content() {
            return String.format("%s%d to %s", bonus >= 0 ? "+" : "-", bonus, flavor);
        }

    }

    /**
     * This indicates to double the result of a particular flavor, like 'slashing x
     * 2'
     */
    public record DoubleDice<E extends Enum<E>>(E flavor) implements DiceMods<E> {

        /**
         * Creates a DoubleDice Dice Mod
         * 
         * @param flavor
         */
        public DoubleDice {
            Preconditions.checkNotNull(flavor, "flavor cannot be null for doubling dice");
        }

        @Override
        public DiceSet<E> apply(DiceSet<E> t) {
            final ImmutableTable<E, DieType, Byte> allDice = t.allDice();
            DiceSetBuilder<E> builder = t.toBuilder();
            ImmutableMap<DieType, Byte> row = allDice.row(flavor);
            for (Entry<DieType, Byte> entry : row.entrySet()) {
                builder.addDie(entry.getKey(), entry.getValue(), flavor);
            }
            builder.addNote(String.format("(doubled dice of %s)", flavor));
            return builder.build();
        }

        @Override
        public String content() {
            return String.format("doubles %s", flavor);
        }
    }

    /**
     * This adds dice to the result, like '+1d6 slashing'
     */
    public record MoreDice<E extends Enum<E>>(E flavor, DieType type, byte count) implements DiceMods<E> {
        /**
         * Creates a MoreDice DiceMod
         * 
         * @param flavor
         * @param type
         * @param count
         */
        public MoreDice {
            Preconditions.checkNotNull(flavor, "flavor cannot be null for adding dice");
            Preconditions.checkNotNull(type, "DieType must not be null");
            Preconditions.checkArgument(count > 0, "count must be >= 0");
        }

        @Override
        public DiceSet<E> apply(DiceSet<E> t) {
            return t.toBuilder().addDie(type, count, flavor)
                    .addNote(String.format("(added %s %d%s)", Plain.UNFLAVORED.equals(flavor) ? "extra" : flavor, count,
                            DieType.ONE.equals(type) ? " bonus" : String.format("d%d", type.getType())))
                    .build();
        }

        @Override
        public String content() {
            return String.format("adds %s %d%s", Plain.UNFLAVORED.equals(flavor) ? "extra" : flavor, count,
                    DieType.ONE.equals(type) ? " bonus" : String.format("d%d", type.getType()));
        }
    }

}
