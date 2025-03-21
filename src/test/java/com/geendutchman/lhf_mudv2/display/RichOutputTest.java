package com.geendutchman.lhf_mudv2.display;

import java.io.StringWriter;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

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

    @Test
    void testDocument() {
        RichOutput output = RichOutput.builder().setSequenceName("Greetings").addString("I am miss nesbit")
                .addOutput(RichOutput.builder().setSequenceName("WhoI").addString("myself").build())
                .addString("Goodbye").build();
        StringWriter writer = new StringWriter();
        Assertions.assertDoesNotThrow(() -> {
            final Document document = output.xmlDocument();
            Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
            for (Map.Entry<String, String> entry : Map
                    .of(OutputKeys.INDENT, "no", OutputKeys.OMIT_XML_DECLARATION, "yes").entrySet()) {
                if (entry == null) {
                    continue;
                }
                final String key = entry.getKey();
                final String value = entry.getValue();
                if (key == null || value == null) {
                    continue;
                }
                transformer.setOutputProperty(key, value);
            }
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            System.out.println(writer.toString());
        });
        Truth.assertThat(writer.toString()).contains("Goodbye");
        Truth.assertThat(writer.toString()).contains("Greetings");
    }

    @Test
    void testPrintit() {
        RichOutput output = RichOutput.builder().setSequenceName("Greetings").addString("I am miss nesbit")
                .addOutput(RichOutput.builder().setSequenceName("WhoI").addString("myself").build())
                .addString("Goodbye").build();
        final String printed = output.printIt();
        Truth.assertThat(printed).contains("Goodbye");
        Truth.assertThat(printed).contains("Greetings");
    }
}
