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

/**
 * This is a set of dice, delineated by some enum of a flavor of some kind. The
 * default flavor is {@link Plain}
 * 
 * @see Plain
 */
@AutoValue
public abstract class DiceSet<E extends Enum<E>> implements Taggable {

    final static NavigableMap<String, String> BASIC_PROPERTIES = Taggable.produceBasicTagProperties();

    @Override
    public ImmutableSortedMap<String, String> properties() {
        return ImmutableSortedMap.copyOf(BASIC_PROPERTIES);
    }

    /**
     * Gets the dice in this dice set as an immutable table
     * 
     * @return immutable table of dice
     */
    public abstract ImmutableTable<E, DieType, Byte> allDice();

    /**
     * Retrieves any note associated with this dice set
     * 
     * @return optional note
     */
    public abstract Optional<String> note();

    /**
     * Retrieves which flavors are associated with this set of dice
     * 
     * @return set of flavors
     */
    public ImmutableSet<E> flavors() {
        return this.allDice().rowKeySet();
    }

    final static Taggable.Tag DICE_SET_TAG = new Taggable.Tag("DICE_SET");

    @Override
    public Taggable.Tag tag() {
        return DICE_SET_TAG;
    }

    /**
     * How many dice are in this set
     * 
     * @return size
     */
    public int size() {
        return this.allDice().size();
    }

    /**
     * Are there dice in this set
     * 
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return this.allDice().isEmpty();
    }

    /**
     * Describes a flavor as a string
     * 
     * @param row
     * @return
     */
    public String rowContent(final E row) {
        StringJoiner sj = new StringJoiner("+", Plain.UNFLAVORED.equals(row) ? "(" : String.format("(%s:", row), ")")
                .setEmptyValue("0");
        final ImmutableMap<DieType, Byte> rowValue = this.allDice().row(row);
        for (final Entry<DieType, Byte> entry : rowValue.entrySet()) {
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
        final ImmutableTable<E, DieType, Byte> cached = this.allDice();
        for (final E row : cached.rowKeySet()) {
            outer.add(this.rowContent(row));
        }

        return outer.toString() + this.note().orElse("");
    }

    /**
     * Roll the set of dice
     * 
     * @return {@link RollSet}
     */
    public RollSet<E> roll() {
        ImmutableSortedMap.Builder<E, Integer> rolls = ImmutableSortedMap.<E, Integer>naturalOrder();
        for (final Entry<E, Map<DieType, Byte>> row : this.allDice().rowMap().entrySet()) {
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

    /**
     * Returns a builder for a particular flavor
     * 
     * @param <E>
     * @return
     */
    public static <E extends Enum<E>> DiceSetBuilder<E> builder() {
        return new AutoValue_DiceSet.Builder<>();
    }

    /**
     * Transform this set of dice into a builder
     * 
     * @return
     */
    public abstract DiceSetBuilder<E> toBuilder();

    /**
     * A builder for a dice set
     */
    @AutoValue.Builder
    public static abstract class DiceSetBuilder<E extends Enum<E>> {

        private final TreeBasedTable<E, DieType, Byte> allDiceBuilder = TreeBasedTable.<E, DieType, Byte>create();

        abstract DiceSetBuilder<E> setAllDice(Table<E, DieType, Byte> retable);

        abstract ImmutableTable<E, DieType, Byte> allDice();

        /**
         * Get whatever note you have set
         * 
         * @return
         */
        public abstract Optional<String> note();

        /**
         * Set a note on the dice set
         * 
         * @param note
         * @return
         */
        public abstract DiceSetBuilder<E> setNote(Optional<String> note);

        /**
         * Append a note to whatever note is present
         * 
         * @param addition
         * @return
         */
        public final DiceSetBuilder<E> addNote(String addition) {
            return this.setNote(Optional.of(this.note().orElse("") + addition));
        }

        /**
         * Remove all dice of a type and flavor
         * 
         * e.g. Remove all slashing d6's.
         * 
         * @param type
         * @param flavor
         * @return
         */
        public final DiceSetBuilder<E> zeroCount(DieType type, E flavor) {
            this.allDiceBuilder.remove(type, flavor);
            // this.setAllDice(allDiceBuilder);
            return this;
        }

        /**
         * Add a dice
         * 
         * @param type   of the dice
         * @param count  of the dice
         * @param flavor of the dice
         * @return
         */
        public final DiceSetBuilder<E> addDie(DieType type, byte count, E flavor) {
            if (count < 0) {
                throw new IllegalArgumentException("cannot subtract dice");
            }
            // this.allDiceBuilder.putAll(this.allDice());
            Byte there = allDiceBuilder.get(flavor, type);
            if (there == null) {
                there = count;
            } else if (there > Byte.MAX_VALUE - count) {
                there = Byte.MAX_VALUE;
            } else if (there < Byte.MIN_VALUE + count) {
                there = Byte.MIN_VALUE;
            } else {
                there = (byte) (there + count);
            }
            allDiceBuilder.put(flavor, type, there);
            this.setAllDice(allDiceBuilder);
            return this;
        }

        /**
         * Add a numerical bonus to the flavor
         * 
         * This is essentially adding d1's
         * 
         * @param flavor
         * @param bonus
         * @return
         */
        public final DiceSetBuilder<E> addBonus(E flavor, byte bonus) {
            this.allDiceBuilder.putAll(this.allDice());
            Byte there = allDiceBuilder.get(flavor, DieType.ONE);
            if (there == null) {
                there = bonus;
            } else if (there > Byte.MAX_VALUE - bonus) {
                there = Byte.MAX_VALUE;
            } else if (there < Byte.MIN_VALUE + bonus) {
                there = Byte.MIN_VALUE;
            } else {
                there = (byte) (there + bonus);
            }
            allDiceBuilder.put(flavor, DieType.ONE, there);
            this.setAllDice(allDiceBuilder);
            return this;
        }

        abstract DiceSet<E> autoBuild();

        /**
         * Build the dice set
         * 
         * @return
         */
        public DiceSet<E> build() {
            Preconditions.checkState(this.allDiceBuilder.size() > 0, "at least one die must be added");
            this.setAllDice(allDiceBuilder);
            Preconditions.checkState(this.allDice().size() > 0, "at least one die must be added");
            return this.autoBuild();
        }
    }

}
