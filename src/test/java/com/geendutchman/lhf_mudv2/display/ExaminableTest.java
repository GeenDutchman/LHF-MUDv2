package com.geendutchman.lhf_mudv2.display;

import java.util.function.Predicate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.common.truth.Truth;

public class ExaminableTest {

    final private static Predicate<String> predicate = Examinable.EXAMINABLE_NAME.asMatchPredicate();

    @ParameterizedTest
    @CsvSource({ "' spaced',false", "sh,false", "acceptable,true", " 'multi word name',true" })
    void testNames(String name, boolean expectPass) {
        boolean result = predicate.test(name);
        Truth.assertWithMessage("match:%s", Examinable.EXAMINABLE_NAME).that(result).isEqualTo(expectPass);
    }
}
