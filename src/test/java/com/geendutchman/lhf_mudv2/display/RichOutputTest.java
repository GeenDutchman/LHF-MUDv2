package com.geendutchman.lhf_mudv2.display;

import org.junit.jupiter.api.Test;

import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;

public class RichOutputTest {
    @Test
    void testElements() {
        final String first = "first";
        final String second = "second";
        RichOutput output = RichOutput.builder().addString(first)
                .addTaggable(
                        new BasicTaggable(new Taggable.Tag("literal"), "before", Taggable.BASIC_TAGGABLE_ATTRIBUTES))
                .addString(second).build();
        RichOutputSubject.assertThat(output).plainString().contains(second);
    }

    @Test
    void testDocument() {
        RichOutput output = RichOutput.builder().setSequenceName("Greetings").addString("I am miss nesbit")
                .addOutput(RichOutput.builder().setSequenceName("WhoI").addString("myself").build())
                .addString("Goodbye").build();
        RichOutputSubject.assertThat(output).asXMLString().contains("Goodbye");
        RichOutputSubject.assertThat(output).asXMLString().contains("Greetings");
    }

    @Test
    void testPrintit() {
        RichOutput output = RichOutput.builder().setSequenceName("Greetings").addString("I am miss nesbit")
                .addOutput(RichOutput.builder().setSequenceName("WhoI").addString("myself").build())
                .addString("Goodbye").build();
        RichOutputSubject.assertThat(output).printed().contains("Goodbye");
        RichOutputSubject.assertThat(output).printed().contains("Greetings");
    }
}
