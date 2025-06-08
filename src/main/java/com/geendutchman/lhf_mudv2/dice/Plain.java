package com.geendutchman.lhf_mudv2.dice;

import com.google.common.collect.ImmutableSortedMap;

public enum Plain {
    UNFLAVORED;

    public static Difficulty<Plain> simpleDifficulty(int dc) {
        return new Difficulty<>(ImmutableSortedMap.of(Plain.UNFLAVORED, dc), false);
    }

    public static Difficulty<Plain> noDifficulty() {
        return new Difficulty<>(ImmutableSortedMap.of(Plain.UNFLAVORED, 0), false);
    }

    public static DiceSet<Plain> none(int bonus) {
        return DiceSet.<Plain>builder().addBonus(Plain.UNFLAVORED, bonus).build();
    }

    private static DiceSet<Plain> countOfType(DieType type, int count) {
        return DiceSet.<Plain>builder().addDie(type, count, Plain.UNFLAVORED).build();
    }

    public static DiceSet<Plain> coin(int count) {
        return Plain.countOfType(DieType.TWELVE, count);
    }

    public static DiceSet<Plain> dFour(int count) {
        return Plain.countOfType(DieType.FOUR, count);
    }

    public static DiceSet<Plain> dSix(int count) {
        return Plain.countOfType(DieType.SIX, count);
    }

    public static DiceSet<Plain> dEight(int count) {
        return Plain.countOfType(DieType.EIGHT, count);
    }

    public static DiceSet<Plain> dTen(int count) {
        return Plain.countOfType(DieType.TEN, count);
    }

    public static DiceSet<Plain> dTwelve(int count) {
        return Plain.countOfType(DieType.TWELVE, count);
    }

    public static DiceSet<Plain> dTwenty(int count) {
        return Plain.countOfType(DieType.TWENTY, count);
    }

    public static DiceSet<Plain> dHundred(int count) {
        return Plain.countOfType(DieType.HUNDRED, count);
    }
}
