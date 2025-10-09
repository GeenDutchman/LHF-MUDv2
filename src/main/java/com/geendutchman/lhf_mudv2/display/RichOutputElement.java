package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Objects;

import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;

/**
 * An element for the Rich Output
 */
public sealed interface RichOutputElement extends Serializable {

    /**
     * Creates an element that is only a string
     * 
     * @param charSequence
     * @return RichOutputElement
     */
    public static RichOutputElement ofString(String charSequence) {
        return new StringElement(charSequence);
    }

    /**
     * Returns an element that holds a taggable
     * 
     * @param taggable
     * @return
     */
    public static RichOutputElement ofTaggable(Taggable taggable) {
        return new TaggableElement(taggable.basicTaggable());
    }

    /**
     * Returns an element that holds an examinable
     * 
     * @param examinable
     * @return
     */
    public static RichOutputElement ofExaminable(Examinable examinable) {
        return new ExaminableElement(examinable.basicExaminable());
    }

    /**
     * Makes a metasignal that is not usually rendered
     * 
     * @param signal
     * @return
     */
    public static RichOutputElement ofSignal(String signal) {
        return new SignalElement(signal);
    }

    /**
     * Nests a RichOutput in an element
     * 
     * @param output
     * @return
     */
    public static RichOutputElement ofNested(RichOutput output) {
        return new NestedElement(output);
    }

    /**
     * Holds a String
     */
    public static record StringElement(String charSequence) implements RichOutputElement {

        public StringElement {
            Objects.requireNonNull(charSequence, "char sequence should not be null");
        }

    }

    /**
     * Holds a taggable
     */
    public static record TaggableElement(BasicTaggable taggable) implements RichOutputElement {

        public TaggableElement {
            Objects.requireNonNull(taggable, "taggable should not be null");
        }

    }

    /**
     * Holds an examinable
     */
    public static record ExaminableElement(BasicExaminable examinable) implements RichOutputElement {

        public ExaminableElement {
            Objects.requireNonNull(examinable, "examinable should not be null");
        }

    }

    /**
     * Holds a meta signal
     */
    public static record SignalElement(String signal) implements RichOutputElement {

        public SignalElement {
            Objects.requireNonNull(signal, "signal should not be null");
        }

    }

    /**
     * Nests into a RichOutput
     */
    public static record NestedElement(RichOutput nested) implements RichOutputElement {

        public NestedElement {
            Objects.requireNonNull(nested, "nested output should not be null");
        }

    }
}
