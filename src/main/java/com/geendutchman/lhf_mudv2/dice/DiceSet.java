package com.geendutchman.lhf_mudv2.dice;

import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.StringJoiner;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

@AutoValue
public abstract class DiceSet<E extends Enum<E>> implements Taggable {

    final static NavigableMap<String, String> BASIC_ATTRIBUTES = Taggable.produceBasicTagAttributes();

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(BASIC_ATTRIBUTES);
    }

    public abstract ImmutableTable<E, DieType, Integer> allDice();

    public abstract Optional<String> note();

    public ImmutableSet<E> flavors() {
        return this.allDice().rowKeySet();
    }

    final static Taggable.Tag DICE_SET_TAG = new Taggable.Tag("DICE_SET");

    @Override
    public Taggable.Tag tag() {
        return DICE_SET_TAG;
    }

    public int size() {
        return this.allDice().size();
    }

    public boolean isEmpty() {
        return this.allDice().isEmpty();
    }

    public String rowContent(final E row) {
        StringJoiner sj = new StringJoiner("+", Plain.UNFLAVORED.equals(row) ? "(" : String.format("(%s:", row), ")")
                .setEmptyValue("0");
        final ImmutableMap<DieType, Integer> rowValue = this.allDice().row(row);
        for (final Entry<DieType, Integer> entry : rowValue.entrySet()) {
            if (DieType.ONE.equals(entry.getKey())) {
                sj.add(entry.getValue().toString());
            } else {
                sj.add(String.format("%dd%d", entry.getValue(), entry.getKey().getType()));
            }
        }
        return sj.toString();
    }

    @Override
    public String content() {
        StringJoiner outer = new StringJoiner("+");
        final ImmutableTable<E, DieType, Integer> cached = this.allDice();
        for (final E row : cached.rowKeySet()) {
            outer.add(this.rowContent(row));
        }

        return outer.toString() + this.note().orElse("");
    }

    public RollSet<E> roll() {
        ImmutableSortedMap.Builder<E, Integer> rolls = ImmutableSortedMap.<E, Integer>naturalOrder();
        for (final Entry<E, Map<DieType, Integer>> row : this.allDice().rowMap().entrySet()) {
            int amount = row.getValue().entrySet().stream().mapToInt(entry -> {
                if (DieType.ONE.equals(entry.getKey())) {
                    return entry.getValue();
                }
                return Roller.BASE.roll(entry.getValue(), entry.getKey());
            }).sum();
            rolls.put(row.getKey(), amount);
        }
        return new RollSet<>(this, rolls.build(), ImmutableMap.of(), Optional.empty());
    }

    public static <E extends Enum<E>> DiceSetBuilder<E> builder() {
        return new AutoValue_DiceSet.Builder<>();
    }

    public abstract DiceSetBuilder<E> toBuilder();

    @AutoValue.Builder
    public static abstract class DiceSetBuilder<E extends Enum<E>> {

        private final TreeBasedTable<E, DieType, Integer> allDiceBuilder = TreeBasedTable.<E, DieType, Integer>create();

        abstract DiceSetBuilder<E> setAllDice(Table<E, DieType, Integer> retable);

        abstract ImmutableTable<E, DieType, Integer> allDice();

        public abstract Optional<String> note();

        public abstract DiceSetBuilder<E> setNote(Optional<String> note);

        public final DiceSetBuilder<E> addNote(String addition) {
            return this.setNote(Optional.of(this.note().orElse("") + addition));
        }

        public final DiceSetBuilder<E> zeroCount(DieType type, E flavor) {
            this.allDiceBuilder.remove(type, flavor);
            // this.setAllDice(allDiceBuilder);
            return this;
        }

        public final DiceSetBuilder<E> addDie(DieType type, int count, E flavor) {
            if (count < 0) {
                throw new IllegalArgumentException("cannot subtract dice");
            }
            // this.allDiceBuilder.putAll(this.allDice());
            Integer there = allDiceBuilder.get(flavor, type);
            if (there != null) {
                there = there + count;
            } else {
                there = count;
            }
            allDiceBuilder.put(flavor, type, there);
            this.setAllDice(allDiceBuilder);
            return this;
        }

        public final DiceSetBuilder<E> addBonus(E flavor, int bonus) {
            this.allDiceBuilder.putAll(this.allDice());
            Integer there = allDiceBuilder.get(flavor, DieType.ONE);
            if (there != null) {
                there = there + bonus;
            } else {
                there = bonus;
            }
            allDiceBuilder.put(flavor, DieType.ONE, there);
            this.setAllDice(allDiceBuilder);
            return this;
        }

        abstract DiceSet<E> autoBuild();

        public DiceSet<E> build() {
            Preconditions.checkState(this.allDiceBuilder.size() > 0, "at least one die must be added");
            this.setAllDice(allDiceBuilder);
            Preconditions.checkState(this.allDice().size() > 0, "at least one die must be added");
            return this.autoBuild();
        }
    }

}
