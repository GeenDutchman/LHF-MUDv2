package com.geendutchman.lhf_mudv2.dice;

import org.junit.jupiter.api.Test;

import com.google.common.truth.Truth;

public class DiceTest {
    private enum Stuff {
        TEST;
    }

    @Test
    public void TestDiceDisplay() {
        final DiceSet<Plain> dice = Plain.dTwenty(1);
        Truth.assertThat(dice).isNotNull();
        Truth.assertThat(dice.content()).containsMatch("1d20");
        System.out.println(dice);

        final RollSet<Plain> result = dice.roll();
        Truth.assertThat(result.content()).containsMatch("1d20");
        System.out.println(result);

        final DiceSet<Stuff> lil = DiceSet.<Stuff>builder().addDie(DieType.EIGHT, 1, Stuff.TEST).build();
        Truth.assertThat(lil).isNotNull();
        final RollSet<Stuff> lilResult = lil.roll();
        Truth.assertThat(lilResult.content()).containsMatch("1d8");
        System.out.println(lilResult);

    }
}
