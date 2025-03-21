package com.geendutchman.lhf_mudv2.display;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.geendutchman.lhf_mudv2.display.RichOutput.OutputBuilderConversionError;
import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

public final class RichOutputSubject extends Subject {

    public static Factory<RichOutputSubject, RichOutput> richOutputs() {
        return RichOutputSubject::new;
    }

    public static RichOutputSubject assertThat(RichOutput actual) {
        return Truth.assertAbout(richOutputs()).that(actual);
    }

    private final RichOutput actual;

    private RichOutputSubject(FailureMetadata metadata, RichOutput actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    public StringSubject asXMLString() {
        try {
            return check("asXMLString()").that(this.actual.xmlString());
        } catch (OutputBuilderConversionError | TransformerException | ParserConfigurationException
                | IllegalArgumentException e) {
            failWithActual(Fact.fact("Failed the printing to XML", e));
            // need to return a fallback
            return this.printed();
        }
    }

    public StringSubject printed() {
        return check("printIt()").that(this.actual.printIt());
    }

    public StringSubject plainString() {
        return check("toString()").that(this.actual.toString());
    }

    public OptionalSubject sequenceName() {
        return check("builderName()").that(actual.sequenceName());
    }

    public IterableSubject elements() {
        return check("elements()").that(actual.elements());
    }

}
