package com.geendutchman.lhf_mudv2.dice;

import java.util.Random;

/**
 * Just the roller for the dice
 */
enum Roller {
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
        final int type = dieType != null ? dieType.getType() : 0;
        if (type <= 0) {
            return 0;
        }
        int result = 1 * numDice; // each die has a minimum of one
        if (numDice > 0) {
            for (int i = 0; i < numDice; i++) {
                result += rand.nextInt(type);
            }
        } else {
            for (int i = 0; i > numDice; i--) {
                result -= rand.nextInt(type);
            }
        }

        return result;
    }
}