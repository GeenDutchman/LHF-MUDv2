package com.geendutchman.lhf_mudv2.dice;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.collect.ImmutableSortedMap;

public record Dice<E extends Enum<E>>(DieType type, int count, E flavor) implements Comparable<Dice<E>>, Taggable {
    public enum Plain {
        UNFLAVORED;
    }

    private final static NavigableMap<String, String> BASIC_ATTRIBUTES = Taggable.produceBasicTagAttributes();

    private enum Roller {
        BASE;

        final private Random rand = new Random();

        /**
         * Rolls so many of one type of die
         *
         * @param numDice how many dice to roll
         * @param dieType what type of die
         * @return the result of the roll
         */
        public int roll(int numDice, DieType dieType) {
            if (dieType.getType() <= 0) {
                return 0;
            }
            int result = 1 * numDice;
            for (int i = 0; i < numDice; i++) {
                result += rand.nextInt(dieType.getType());
            }
            return result;
        }
    }

    public Dice {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(flavor, "flavor must not be null");
    }

    public Roll<E> roll() {
        int result = Roller.BASE.roll(count, type);
        return new Roll<>(this, result);
    }

    @Override
    public int compareTo(Dice<E> o) {
        if (o == null) {
            throw new NullPointerException("other Dice is null");
        }
        int flavorCompare = this.flavor.name().compareTo(o.flavor.name());
        if (flavorCompare != 0) {
            return flavorCompare;
        }
        int typeCompare = this.type.compareTo(o.type);
        if (typeCompare != 0) {
            return typeCompare;
        }
        return this.count - o.count;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.count).append("d").append(this.type.getType());
        if (!this.flavor.equals(Plain.UNFLAVORED)) {
            sb.append(" ").append(this.flavor.name());
        }
        return sb.toString();
    }

    public static Dice<Plain> none(int count) {
        return new Dice<Plain>(DieType.NONE, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> two(int count) {
        return new Dice<Plain>(DieType.TWO, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> four(int count) {
        return new Dice<Plain>(DieType.FOUR, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> six(int count) {
        return new Dice<Plain>(DieType.SIX, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> eight(int count) {
        return new Dice<Plain>(DieType.EIGHT, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> ten(int count) {
        return new Dice<Plain>(DieType.TEN, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> twelve(int count) {
        return new Dice<Plain>(DieType.TWELVE, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> twenty(int count) {
        return new Dice<Plain>(DieType.TWENTY, count, Plain.UNFLAVORED);
    }

    public static Dice<Plain> hundred(int count) {
        return new Dice<Plain>(DieType.HUNDRED, count, Plain.UNFLAVORED);
    }

    @Override
    public String tag() {
        return "DICE";
    }

    @Override
    public String content() {
        return this.toString();
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(Dice.BASIC_ATTRIBUTES);
    }
}
