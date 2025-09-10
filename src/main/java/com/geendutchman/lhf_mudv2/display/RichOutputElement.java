package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Map.Entry;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutput.OutputBuilderConversionError;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.geendutchman.lhf_mudv2.display.Taggable.Tag;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * An element for the Rich Output
 */
public abstract class RichOutputElement implements Serializable {

    /**
     * Generates a node for xml
     * 
     * @param nodeFactory what can create the nodes
     * @param parent      the parent node to which to attach
     * @throws OutputBuilderConversionError if we cannot build it
     */
    abstract void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError;

    /**
     * Consolidates a string to build
     * 
     * @param builder
     */
    abstract void printIt(final StringBuilder builder);

    /**
     * Creates an element that is only a string
     * 
     * @param charSequence
     * @return RichOutputElement
     */
    public static RichOutputElement ofString(String charSequence) {
        return new AutoValue_RichOutputElement_StringElement(charSequence);
    }

    /**
     * Returns an element that holds a taggable
     * 
     * @param taggable
     * @return
     */
    public static RichOutputElement ofTaggable(Taggable taggable) {
        return new AutoValue_RichOutputElement_TaggableElement(taggable.basicTaggable());
    }

    /**
     * Returns an element that holds an examinable
     * 
     * @param examinable
     * @return
     */
    public static RichOutputElement ofExaminable(Examinable examinable) {
        return new AutoValue_RichOutputElement_ExaminableElement(examinable.basicExaminable());
    }

    /**
     * Makes a metasignal that is not usually rendered
     * 
     * @param signal
     * @return
     */
    public static RichOutputElement ofSignal(String signal) {
        return new AutoValue_RichOutputElement_SignalElement(signal);
    }

    /**
     * Nests a RichOutput in an element
     * 
     * @param output
     * @return
     */
    public static RichOutputElement ofNested(RichOutput output) {
        return new AutoValue_RichOutputElement_NestedElement(output);
    }

    /**
     * Holds a String
     */
    @AutoValue
    public static abstract class StringElement extends RichOutputElement {
        public abstract String charSequence();

        @Override
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
            try {
                parent.appendChild(nodeFactory.createTextNode(this.charSequence()));
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(
                        String.format("Error for element while appending text: %s", this), e);
            }
        }

        @Override
        final void printIt(final StringBuilder builder) {
            builder.append(this.charSequence());
        }

    }

    /**
     * Holds a taggable
     */
    @AutoValue
    public static abstract class TaggableElement extends RichOutputElement {
        public abstract BasicTaggable taggable();

        @Override
        final void printIt(final StringBuilder builder) {
            builder.append(this.taggable().content());
        }

        @Override
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
            final BasicTaggable taggable = this.taggable();
            Element myElement = null;
            try {
                final Tag tag = taggable.tag();
                myElement = nodeFactory.createElement(
                        tag == null || tag.value() == null || tag.value().isBlank() ? "Taggable" : tag.value());
                parent.appendChild(myElement);
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(String.format(
                        "Error either creating element (with the Taggable TagName of '%s') or appending it to the current node",
                        taggable.tag()), e);
            }

            try {
                myElement.appendChild(nodeFactory.createTextNode(taggable.content()));
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(
                        String.format("Error appending child text node for Taggable %s", taggable.content()), e);
            }

            for (final Entry<String, String> entry : taggable.attributes().entrySet()) {
                try {
                    myElement.setAttribute(entry.getKey(), entry.getValue());
                } catch (DOMException e) {
                    throw new OutputBuilderConversionError(
                            String.format("Error setting attribute '%s=%s' for Taggable %s", entry.getKey(),
                                    entry.getValue(), taggable.content()),
                            e);
                }
            }
        }
    }

    /**
     * Holds an examinable
     */
    @AutoValue
    public static abstract class ExaminableElement extends RichOutputElement {
        public abstract BasicExaminable examinable();

        @Override
        final void printIt(final StringBuilder builder) {
            final BasicExaminable examined = this.examinable();
            builder.append(examined.name()).append(":\n");
            if (!examined.content().equals(examined.name())) {
                builder.append("\t").append(examined.content()).append("\n");
            }
            if (examined.description().isPresent()) {
                final RichOutputElement description = RichOutputElement.ofNested(examined.description().get());
                StringBuilder child = new StringBuilder();
                description.printIt(child);
                for (final String line : child.toString().split("\\r?\\n")) {
                    builder.append("\t").append(line).append("\n");
                }
            }
        }

        @Override
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
            final BasicExaminable examined = this.examinable();
            Element myElement = null;
            try {
                final Tag tag = examined.tag();
                myElement = nodeFactory.createElement(
                        tag == null || tag.value() == null || tag.value().isBlank() ? "Examinable" : tag.value());
                parent.appendChild(myElement);
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(String.format(
                        "Error either creating element (with the Examinable TagName of '%s') or appending it to the current node",
                        examined.tag()), e);
            }

            if (!examined.name().equals(examined.content())) {
                final RichOutputElement nameTaggable = RichOutputElement.ofTaggable(BasicTaggable
                        .customTaggable(new Tag("name"), examined.name(), Taggable.produceBasicTagAttributes()));
                try {
                    nameTaggable.xmlNode(nodeFactory, myElement);
                    myElement.appendChild(nodeFactory.createTextNode(examined.content()));
                } catch (OutputBuilderConversionError e) {
                    throw new OutputBuilderConversionError(
                            String.format("Error accepting name Taggable '%s' for the Examinable", nameTaggable), e);
                }
            } else {
                try {
                    myElement.appendChild(nodeFactory.createTextNode(examined.content()));
                } catch (OutputBuilderConversionError e) {
                    throw new OutputBuilderConversionError("Error adding Examinable content", e);
                }
            }

            if (examined.description().isPresent()) {
                final RichOutputElement description = RichOutputElement.ofNested(examined.description().get());
                try {
                    description.xmlNode(nodeFactory, myElement);
                } catch (OutputBuilderConversionError e) {
                    throw new OutputBuilderConversionError("Error accepting Examinable description", e);
                }
            }

            for (final Entry<String, String> entry : examined.attributes().entrySet()) {
                try {
                    myElement.setAttribute(entry.getKey(), entry.getValue());
                } catch (DOMException e) {
                    throw new OutputBuilderConversionError(
                            String.format("Error setting attribute '%s=%s' for Examinable %s", entry.getKey(),
                                    entry.getValue(), examined.name()),
                            e);
                }
            }
        }
    }

    /**
     * Holds a meta signal
     */
    @AutoValue
    public static abstract class SignalElement extends RichOutputElement {
        public abstract String signal();

        @Override
        final void printIt(final StringBuilder builder) {
        }

        @Override
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
        }
    }

    /**
     * Nests into a RichOutput
     */
    @AutoValue
    public static abstract class NestedElement extends RichOutputElement {
        public abstract RichOutput nested();

        @Override
        final void printIt(final StringBuilder builder) {
            final RichOutput output = this.nested();
            output.sequenceName().ifPresent(seqName -> builder.append(seqName).append(":\n\t"));
            for (final RichOutputElement element : output.elements()) {
                if (element == null) {
                    continue;
                }
                StringBuilder child = new StringBuilder();
                element.printIt(child);
                final String[] splitten = child.toString().split("\r?\n");
                if (splitten == null) {
                    continue;
                }
                if (splitten.length > 1) {
                    for (final String line : splitten) {
                        if (output.sequenceName().isPresent()) {
                            builder.append("\t");
                        }
                        builder.append(line).append("\n");
                    }
                } else if (splitten.length == 1) {
                    builder.append(child);
                    output.elementSeparator().ifPresent(sep -> sep.printIt(builder));
                }
            }
        }

        @Override
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
            final RichOutput output = this.nested();
            Element root = null;
            try {
                final String tag = output.tag().orElse("output");
                root = nodeFactory.createElement(tag);
                parent.appendChild(root);
                if (output.sequenceName().isPresent()) {
                    final RichOutputElement sequenceTaggable = RichOutputElement
                            .ofTaggable(BasicTaggable.customTaggable(new Tag(tag + "-title"),
                                    output.sequenceName().get(), Taggable.produceBasicTagAttributes()));
                    sequenceTaggable.xmlNode(nodeFactory, root);
                }
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(String.format(
                        "Error either creating element (with the Output name of '%s') or appending it to the document",
                        output.sequenceName()), e);
            }

            for (final Entry<String, String> entry : output.attributes().entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                if (key != null && value != null) {
                    root.setAttribute(key, value);
                }
            }

            final ImmutableList<RichOutputElement> retrieved = output.elements();
            if (retrieved.isEmpty()) {
                if (output.onEmpty().isPresent()) {
                    root.appendChild(nodeFactory.createTextNode(output.onEmpty().orElse("")));
                }
            } else {
                for (int i = 0; i < retrieved.size(); i++) {
                    final RichOutputElement element = retrieved.get(i);
                    element.xmlNode(nodeFactory, root);
                    if (i < retrieved.size() - 1 && output.elementSeparator().isPresent()) {
                        output.elementSeparator().get().xmlNode(nodeFactory, root);
                    }
                    if (i == retrieved.size() - 2 && output.isAndLast()) {
                        RichOutputElement.ofString("and ").xmlNode(nodeFactory, root);
                    }
                }
            }
        }
    }
}
