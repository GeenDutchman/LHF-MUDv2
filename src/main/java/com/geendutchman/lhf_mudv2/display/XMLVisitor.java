package com.geendutchman.lhf_mudv2.display;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.geendutchman.lhf_mudv2.display.RichOutputElement.ExaminableElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.NestedElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.SignalElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.StringElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.TaggableElement;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.geendutchman.lhf_mudv2.display.Taggable.Tag;
import com.google.common.collect.ImmutableList;
import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;

public final class XMLVisitor implements RichOutputElementVisitor {
    private final Document document;
    private Element root;
    private Element current;

    private XMLVisitor() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.document = documentBuilder.newDocument();
        this.root = null;
        try {
            root = document.createElement("DOCUMENT");
            document.appendChild(root);
        } catch (DOMException e) {
            throw new OutputBuilderConversionError(String.format("Error creating document"), e);
        }
        this.current = root;
    }

    public Document xmlDocument() {
        return this.document;
    }

    public static Document buildXML(final RichOutput output)
            throws ParserConfigurationException, OutputBuilderConversionError {
        final XMLVisitor myself = new XMLVisitor();
        final NestedElement nested = new NestedElement(output);
        myself.visit(nested);
        return myself.xmlDocument();
    }

    public static void writeXML(final RichOutput output, final Writer writer)
            throws ParserConfigurationException, TransformerException, OutputBuilderConversionError {
        final Document doc = buildXML(output);
        Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
        for (Map.Entry<String, String> entry : Map.of(OutputKeys.INDENT, "no", OutputKeys.OMIT_XML_DECLARATION, "yes")
                .entrySet()) {
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
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
    }

    public static String xmlString(final RichOutput output)
            throws ParserConfigurationException, TransformerException, OutputBuilderConversionError {
        StringWriter writer = new StringWriter();
        XMLVisitor.writeXML(output, writer);
        return writer.toString();
    }

    @Override
    public void visit(final StringElement element) {
        try {
            this.current.appendChild(this.document.createTextNode(element.charSequence()));
        } catch (Exception e) {
            throw new OutputBuilderConversionError(String.format("Error for element while appending text: %s", this),
                    e);
        }
    }

    @Override
    public void visit(final TaggableElement taggableElement) {
        final BasicTaggable taggable = taggableElement.taggable();
        Element myElement = null;
        try {
            final Tag tag = taggable.tag();
            myElement = this.document.createElement(
                    tag == null || tag.value() == null || tag.value().isBlank() ? "Taggable" : tag.value());
            this.current.appendChild(myElement);
        } catch (DOMException e) {
            throw new OutputBuilderConversionError(String.format(
                    "Error either creating element (with the Taggable TagName of '%s') or appending it to the current node",
                    taggable.tag()), e);
        }

        try {
            myElement.appendChild(this.document.createTextNode(taggable.content()));
        } catch (DOMException e) {
            throw new OutputBuilderConversionError(
                    String.format("Error appending child text node for Taggable %s", taggable.content()), e);
        }

        for (final Entry<String, String> entry : taggable.properties().entrySet()) {
            try {
                myElement.setAttribute(entry.getKey(), entry.getValue());
            } catch (DOMException e) {
                throw new OutputBuilderConversionError(String.format("Error setting attribute '%s=%s' for Taggable %s",
                        entry.getKey(), entry.getValue(), taggable.content()), e);
            }
        }
    }

    @Override
    public void visit(final ExaminableElement examinable) {
        final BasicExaminable examined = examinable.examinable();
        Element myElement = null;
        try {
            final Tag tag = examined.tag();
            myElement = this.document.createElement(
                    tag == null || tag.value() == null || tag.value().isBlank() ? "Examinable" : tag.value());
            this.current.appendChild(myElement);
        } catch (DOMException e) {
            throw new OutputBuilderConversionError(String.format(
                    "Error either creating element (with the Examinable TagName of '%s') or appending it to the current node",
                    examined.tag()), e);
        }

        if (!examined.name().toString().equals(examined.content())) {
            final TaggableElement nameTaggable = new TaggableElement(
                    new BasicTaggable(new Tag("name"), examined.name().toString(), Taggable.BASIC_TAGGABLE_PROPERTIES));
            try {
                Element temp = this.current;
                this.current = myElement;
                this.visit(nameTaggable);
                // nameTaggable.xmlNode(nodeFactory, myElement);
                this.current = temp;
                myElement.appendChild(this.document.createTextNode(examined.content()));
            } catch (OutputBuilderConversionError e) {
                throw new OutputBuilderConversionError(
                        String.format("Error accepting name Taggable '%s' for the Examinable", nameTaggable), e);
            }
        } else {
            try {
                myElement.appendChild(this.document.createTextNode(examined.content()));
            } catch (OutputBuilderConversionError e) {
                throw new OutputBuilderConversionError("Error adding Examinable content", e);
            }
        }

        if (examined.description().isPresent()) {
            final NestedElement description = new NestedElement(examined.description().get());
            try {
                Element temp = this.current;
                this.current = myElement;
                this.visit(description);
                // description.xmlNode(nodeFactory, myElement);
                this.current = temp;
            } catch (OutputBuilderConversionError e) {
                throw new OutputBuilderConversionError("Error accepting Examinable description", e);
            }
        }

        for (final Entry<String, String> entry : examined.properties().entrySet()) {
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

    @Override
    public void visit(final SignalElement signal) {

    }

    @Override
    public void visit(final NestedElement nestedElement) {
        final RichOutput output = nestedElement.nested();
        Element nroot = null;
        try {
            final Tag tag = output.tag().orElse(new Taggable.Tag("output"));
            nroot = this.document.createElement(tag.value());
            this.current.appendChild(nroot);
            if (output.sequenceName().isPresent()) {
                final TaggableElement sequenceTaggable = new TaggableElement(
                        new BasicTaggable(new Taggable.Tag(tag.value() + "-title"), output.sequenceName().get().value(),
                                Taggable.BASIC_TAGGABLE_PROPERTIES));
                Element temp = this.current;
                this.current = nroot;
                this.visit(sequenceTaggable);
                // sequenceTaggable.xmlNode(nodeFactory, nroot);
                this.current = temp;
            }
        } catch (DOMException e) {
            throw new OutputBuilderConversionError(String.format(
                    "Error either creating element (with the Output name of '%s') or appending it to the document",
                    output.sequenceName()), e);
        }

        for (final Entry<String, String> entry : output.properties().entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key != null && value != null) {
                nroot.setAttribute(key, value);
            }
        }

        final ImmutableList<RichOutputElement> retrieved = output.elements();
        if (retrieved.isEmpty()) {
            if (output.onEmpty().isPresent()) {
                nroot.appendChild(this.document.createTextNode(output.onEmpty().orElse("")));
            }
        } else {
            Element temp = this.current;
            this.current = nroot;
            for (int i = 0; i < retrieved.size(); i++) {
                final RichOutputElement element = retrieved.get(i);
                switch (element) {
                case RichOutputElement.StringElement se -> {
                    this.visit(se);
                }
                case RichOutputElement.TaggableElement te -> {
                    this.visit(te);
                }
                case RichOutputElement.ExaminableElement ee -> {
                    this.visit(ee);
                }
                case RichOutputElement.SignalElement se -> {
                    this.visit(se);
                }
                case RichOutputElement.NestedElement ne -> {
                    this.visit(ne);
                }
                case null -> {
                }
                default -> {
                }

                }
                // element.xmlNode(nodeFactory, nroot);
                if (i < retrieved.size() - 1 && output.elementSeparator().isPresent()) {
                    switch (output.elementSeparator().get()) {
                    case RichOutputElement.StringElement se -> {
                        this.visit(se);
                    }
                    case RichOutputElement.TaggableElement te -> {
                        this.visit(te);
                    }
                    case RichOutputElement.ExaminableElement ee -> {
                        this.visit(ee);
                    }
                    case RichOutputElement.SignalElement se -> {
                        this.visit(se);
                    }
                    case RichOutputElement.NestedElement ne -> {
                        this.visit(ne);
                    }
                    case null -> {
                    }
                    default -> {
                    }

                    }
                }
                if (i == retrieved.size() - 2 && output.isAndLast()) {
                    this.visit(new StringElement("and "));
                }
            }
            this.current = temp;
        }
    }

}
