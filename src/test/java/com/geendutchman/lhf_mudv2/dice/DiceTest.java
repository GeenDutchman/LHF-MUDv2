package com.geendutchman.lhf_mudv2.dice;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.common.truth.Truth;

@SpringBootTest
public class DiceTest {
    private enum Stuff {
        TEST;
    }

    @Test
    public void TestDiceDisplay() {
        final Map<String, String> prevMDC = MDC.getCopyOfContextMap();
        try {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.atTrace().log("old MDC {}", MDC.getCopyOfContextMap());
            MDC.setContextMap(null);
            final DiceSet<Plain> dice = Plain.dTwenty((byte) 1);
            Truth.assertThat(dice).isNotNull();
            MDC.put("dice", dice.toString());
            logger.atInfo().log("made dice");
            Truth.assertThat(dice.content()).containsMatch("1d20");

            final RollSet<Plain> result = dice.roll();
            MDC.put("result", result.toString());
            logger.atInfo().log("got result");
            Truth.assertThat(result.content()).containsMatch("1d20");

            final DiceSet<Stuff> lil = DiceSet.<Stuff>builder().addDie(DieType.EIGHT, (byte) 1, Stuff.TEST).build();
            Truth.assertThat(lil).isNotNull();
            final RollSet<Stuff> lilResult = lil.roll();
            logger.atInfo().addKeyValue("lilResult", lilResult.toString());
            Truth.assertThat(lilResult.content()).containsMatch("1d8");
        } finally {
            MDC.setContextMap(prevMDC);
        }

    }
}
