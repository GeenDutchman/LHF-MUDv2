package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Map.Entry;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.geendutchman.lhf_mudv2.display.RichOutput.OutputBuilderConversionError;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.google.auto.value.AutoValue;

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

    public static RichOutputElement ofString(String charSequence) {
        return new AutoValue_RichOutputElement_StringElement(charSequence);
    }

    public static RichOutputElement ofTaggable(Taggable taggable) {
        return new AutoValue_RichOutputElement_TaggableElement(Taggable.basicTaggable(taggable));
    }

    public static RichOutputElement ofSignal(String signal) {
        return new AutoValue_RichOutputElement_SignalElement(signal);
    }

    public static RichOutputElement ofOutput(RichOutput output) {
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

    }

    /**
     * Holds a taggable
     */
    @AutoValue
    public static abstract class TaggableElement extends RichOutputElement {
        public abstract BasicTaggable taggable();

        @Override
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
            final BasicTaggable taggable = this.taggable();
            Element myElement = null;
            try {
                myElement = nodeFactory.createElement(!taggable.tag().isBlank() ? taggable.tag() : "Taggable");
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
     * Holds a meta signal
     */
    @AutoValue
    public static abstract class SignalElement extends RichOutputElement {
        public abstract String signal();

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
        final void xmlNode(final Document nodeFactory, final Node parent) throws OutputBuilderConversionError {
            final RichOutput output = this.nested();
            Element root = null;
            try {
                root = nodeFactory.createElement(output.tag().orElse("output"));
                parent.appendChild(root);
                root.appendChild(nodeFactory.createTextNode(output.sequenceName().orElse("")));
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(String.format(
                        "Error either creating root element (with the Output name of '%s') or appending it to the document",
                        output.sequenceName()), e);
            }

            for (final RichOutputElement element : output.elements()) {
                try {
                    element.xmlNode(nodeFactory, root);
                } catch (OutputBuilderConversionError e) {
                    throw new OutputBuilderConversionError("Error for nested:", e);
                }
            }
        }
    }
}
