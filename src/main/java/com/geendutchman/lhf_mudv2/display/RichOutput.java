package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

@AutoValue
public abstract class RichOutput implements Serializable {

    public abstract Optional<String> sequenceName();

    public abstract ImmutableList<RichOutputElement> elements();

    public abstract Optional<RichOutputElement> elementSeparator();

    public abstract boolean isAndLast();

    public abstract Optional<String> tag();

    public abstract ImmutableSortedMap<String, String> attributes();

    public abstract Optional<String> onEmpty();

    public static Builder builder() {
        return new AutoValue_RichOutput.Builder().setElementSeparator(Optional.of(RichOutputElement.ofString(" ")))
                .setTag(Optional.of("output")).setIsAndLast(false);
    }

    public final static String SEQUENCE_NAME_PATTERN = "^\\w+";
    public final static String TAG_PATTERN = "^\\w{3}[\\w_-]+\\w$";

    @AutoValue.Builder
    public abstract static class Builder implements Serializable {
        public abstract Builder setSequenceName(Optional<String> sequenceName);

        public abstract Builder setSequenceName(String sequenceName);

        public abstract Builder setElements(Iterable<RichOutputElement> elements);

        public abstract Builder setElements(RichOutputElement... elements);

        public abstract Builder setElementSeparator(Optional<RichOutputElement> separator);

        public abstract Builder setIsAndLast(boolean andLast);

        public abstract Builder setTag(Optional<String> tag);

        abstract ImmutableSortedMap.Builder<String, String> attributesBuilder();

        public final Builder putAttribute(String key, String value) {
            attributesBuilder().put(key, value);
            return this;
        }

        public abstract Builder setOnEmpty(Optional<String> onEmpty);

        abstract ImmutableList.Builder<RichOutputElement> elementsBuilder();

        public final Builder addElement(RichOutputElement element) {
            elementsBuilder().add(element);
            return this;
        }

        public final Builder addString(String charsequence) {
            return this.addElement(RichOutputElement.ofString(charsequence));
        }

        public final Builder addTaggable(Taggable tagged) {
            return this.addElement(RichOutputElement.ofTaggable(tagged));
        }

        public final Builder addExaminable(Examinable examinable) {
            return this.addElement(RichOutputElement.ofExaminable(examinable));
        }

        public final Builder addSignal(String signal) {
            elementsBuilder().add(RichOutputElement.ofSignal(signal));
            return this;
        }

        public final Builder addOutput(RichOutput output) {
            return this.addElement(RichOutputElement.ofOutput(output));
        }

        abstract RichOutput autoBuild();

        public final RichOutput build() {
            RichOutput output = autoBuild();
            Preconditions.checkState(
                    output.sequenceName().isEmpty() || output.sequenceName().get().matches(SEQUENCE_NAME_PATTERN),
                    "sequence name must match: %s", SEQUENCE_NAME_PATTERN);
            Preconditions.checkState(output.tag().isEmpty() || output.tag().get().matches(TAG_PATTERN),
                    "tag must match: %s", TAG_PATTERN);
            return output;
        }
    }

    public final static class OutputBuilderConversionError extends RuntimeException {
        public OutputBuilderConversionError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public final String printIt() {
        final StringBuilder builder = new StringBuilder();
        // Delegate to a RichOutputElement
        final RichOutputElement myself = RichOutputElement.ofOutput(this);
        myself.printIt(builder);
        return builder.toString();
    }

    public final Document xmlDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element root = null;
        try {
            root = document.createElement("DOCUMENT");
            document.appendChild(root);
        } catch (DOMException e) {
            throw new OutputBuilderConversionError(String.format("Error creating document for %s", this.sequenceName()),
                    e);
        }

        try {
            // Delegate to a RichOutputElement
            final RichOutputElement myself = RichOutputElement.ofOutput(this);
            myself.xmlNode(document, root);
        } catch (OutputBuilderConversionError e) {
            throw new OutputBuilderConversionError("Error creating root element", e);
        }

        return document;
    }
}
