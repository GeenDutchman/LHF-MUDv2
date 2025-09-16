package com.geendutchman.lhf_mudv2.dice;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableSortedMap;

public enum D6Set {
    ONE, TWO, THREE, FOUR, FIVE, SIX;

    public static DiceSet<D6Set> makeSet(D6Set ofSize) {
        DiceSet.DiceSetBuilder<D6Set> builder = DiceSet.builder();
        if (ofSize == null) {
            ofSize = FOUR;
        }
        for (D6Set count : D6Set.values()) {
            builder.addDie(DieType.SIX, (byte) 1, ofSize);
            if (count == ofSize) {
                break;
            }
        }
        return builder.build();
    }

    public static RollSet<D6Set> dropLowest(DiceSet<D6Set> set, D6Set count) {
        if (set == null) {
            throw new IllegalArgumentException("provided set must not be null");
        }
        if (count == null) {
            count = ONE;
        }
        RollSet<D6Set> result = set.roll();
        ImmutableSortedMap<D6Set, Integer> individuals = result.rolls();
        for (@SuppressWarnings("unused")
        D6Set _tick : D6Set.values()) {
            Optional<Entry<D6Set, Integer>> min = individuals.entrySet().stream().min(Map.Entry.comparingByValue());
            if (min.isPresent()) {
                result = new RollMods.Zeroed<D6Set>(min.get().getKey()).apply(result);
            }
        }
        return result;
    }

    public static RollSet<D6Set> fourD6DropLowest() {
        DiceSet<D6Set> set = D6Set.makeSet(FOUR);
        RollSet<D6Set> result = D6Set.dropLowest(set, ONE);
        return result;
    }

    public static int fourD6DropLowestAsInt() {
        return D6Set.fourD6DropLowest().result();
    }

    public static byte fourD6DropLowestAsByte() {
        int result = D6Set.fourD6DropLowestAsInt();
        if (result > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        } else if (result < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        }
        return (byte) result;
    }
}
