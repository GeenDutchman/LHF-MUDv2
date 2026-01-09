package com.geendutchman.lhf_mudv2.dice;

import com.google.common.collect.ImmutableSortedMap;

/**
 * This is the default flavor enum for a {@link DiceSet}.
 * 
 * This also includes various utility methods for Plain sets
 */
public enum Plain {
    /**
     * This is the default flavor for a set of dice, best used if you just want to
     * roll a set of dice and get the result sum
     */
    UNFLAVORED;

    /**
     * Make a simple difficulty for something, like DC 10
     * 
     * @param dc
     * @return
     */
    public static Difficulty<Plain> simpleDifficulty(int dc) {
        return new Difficulty<>(ImmutableSortedMap.of(Plain.UNFLAVORED, dc), false);
    }

    /**
     * Make something so easy, DC 0
     * 
     * @return
     */
    public static Difficulty<Plain> noDifficulty() {
        return new Difficulty<>(ImmutableSortedMap.of(Plain.UNFLAVORED, 0), false);
    }

    /**
     * Just a +bonus
     *
     * @see DiceSet.DiceSetBuilder#addBonus
     * @param bonus
     * @return
     */
    public static DiceSet<Plain> none(byte bonus) {
        return DiceSet.<Plain>builder().addBonus(Plain.UNFLAVORED, bonus).build();
    }

    /**
     * Make a simple set of dice, like a 1d6
     * 
     * @param type
     * @param count
     * @return
     */
    private static DiceSet<Plain> countOfType(DieType type, byte count) {
        return DiceSet.<Plain>builder().addDie(type, count, Plain.UNFLAVORED).build();
    }

    /**
     * How many coins do you want to flip
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> coin(byte count) {
        return Plain.countOfType(DieType.TWELVE, count);
    }

    /**
     * How many d4's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dFour(byte count) {
        return Plain.countOfType(DieType.FOUR, count);
    }

    /**
     * How many d6's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dSix(byte count) {
        return Plain.countOfType(DieType.SIX, count);
    }

    /**
     * How many d8's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dEight(byte count) {
        return Plain.countOfType(DieType.EIGHT, count);
    }

    /**
     * How many d10's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dTen(byte count) {
        return Plain.countOfType(DieType.TEN, count);
    }

    /**
     * How many d12's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dTwelve(byte count) {
        return Plain.countOfType(DieType.TWELVE, count);
    }

    /**
     * How many d20's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dTwenty(byte count) {
        return Plain.countOfType(DieType.TWENTY, count);
    }

    /**
     * How many d100's do you want to roll
     * 
     * @param count
     * @return
     */
    public static DiceSet<Plain> dHundred(byte count) {
        return Plain.countOfType(DieType.HUNDRED, count);
    }
}
