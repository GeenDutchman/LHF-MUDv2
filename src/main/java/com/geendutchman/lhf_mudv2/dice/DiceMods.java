package com.geendutchman.lhf_mudv2.dice;

import java.util.Map.Entry;
import java.util.function.UnaryOperator;

import com.geendutchman.lhf_mudv2.dice.DiceSet.DiceSetBuilder;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

public sealed interface DiceMods<E extends Enum<E>> extends UnaryOperator<DiceSet<E>>, Taggable
        permits DiceMods.FlavoredBonus, DiceMods.DoubleDice, DiceMods.MoreDice {

    @Override
    public default Taggable.Tag tag() {
        return new Taggable.Tag("DICE_MODIFIER");
    }

    public record FlavoredBonus<E extends Enum<E>>(E flavor, int bonus) implements DiceMods<E> {

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

    public record DoubleDice<E extends Enum<E>>(E flavor) implements DiceMods<E> {

        public DoubleDice {
            Preconditions.checkNotNull(flavor, "flavor cannot be null for doubling dice");
        }

        @Override
        public DiceSet<E> apply(DiceSet<E> t) {
            final ImmutableTable<E, DieType, Integer> allDice = t.allDice();
            DiceSetBuilder<E> builder = t.toBuilder();
            ImmutableMap<DieType, Integer> row = allDice.row(flavor);
            for (Entry<DieType, Integer> entry : row.entrySet()) {
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

    public record MoreDice<E extends Enum<E>>(E flavor, DieType type, int count) implements DiceMods<E> {
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
