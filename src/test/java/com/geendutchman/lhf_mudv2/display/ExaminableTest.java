package com.geendutchman.lhf_mudv2.display;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.common.truth.Truth;

public class ExaminableTest {

    final private static Predicate<String> predicate = Pattern.compile(Examinable.Name.NAME_PATTERN).asPredicate();

    @ParameterizedTest
    @CsvSource({ "' spaced',false", "sh,false", "acceptable,true", " 'multi word name',true" })
    void testNames(String name, boolean expectPass) {
        boolean result = predicate.test(name);
        Truth.assertWithMessage("match:%s", Examinable.Name.NAME_PATTERN).that(result).isEqualTo(expectPass);
    }
}
