package com.geendutchman.lhf_mudv2.display;

import org.junit.jupiter.api.Test;

import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.google.common.truth.Truth;

public class RichOutputTest {
    @Test
    void testElements() {
        final String first = "first";
        final String second = "second";
        RichOutput output = RichOutput.builder().addString(first)
                .addTaggable(BasicTaggable.customTaggable("literal", "before", Taggable.produceBasicTagAttributes()))
                .addString(second).build();
        Truth.assertThat(output.toString()).contains(second);
    }
}
