package com.geendutchman.lhf_mudv2.dice;

import org.junit.jupiter.api.Test;

import com.geendutchman.lhf_mudv2.dice.Dice.Plain;
import com.google.common.truth.Truth;

public class DiceTest {
    private enum Stuff {
        TEST;
    }

    @Test
    public void TestDiceDisplay() {
        final Dice<Plain> dice = Dice.twenty(1);
        Truth.assertThat(dice).isNotNull();
        Truth.assertThat(dice.content()).containsMatch("1d20");
        System.out.println(dice);

        final Roll<Plain> result = dice.roll();
        Truth.assertThat(result.content()).containsMatch("1d20");
        System.out.println(result);

        final Dice<Stuff> lil = new Dice<DiceTest.Stuff>(DieType.EIGHT, 1, Stuff.TEST);
        Truth.assertThat(lil).isNotNull();
        final Roll<Stuff> lilResult = lil.roll();
        Truth.assertThat(lilResult.content()).containsMatch("1d8");
        System.out.println(lilResult);

    }
}
