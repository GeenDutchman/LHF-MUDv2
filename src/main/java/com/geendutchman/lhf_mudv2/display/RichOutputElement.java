package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;

import com.google.auto.value.AutoValue;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;

/**
 * An element for the Rich Output
 */
public abstract class RichOutputElement implements Serializable {

    public static RichOutputElement ofString(String charSequence) {
        return new AutoValue_RichOutputElement_StringElement(charSequence);
    }

    public static RichOutputElement ofTaggable(Taggable taggable) {
        return new AutoValue_RichOutputElement_TaggableElement(Taggable.basicTaggable(taggable));
    }

    public static RichOutputElement ofSignal(String signal) {
        return new AutoValue_RichOutputElement_SignalElement(signal);
    }

    @AutoValue
    /**
     * Holds a String
     */
    public static abstract class StringElement extends RichOutputElement {
        public abstract String charSequence();

    }

    @AutoValue
    /**
     * Holds a taggable
     */
    public static abstract class TaggableElement extends RichOutputElement {
        public abstract BasicTaggable taggable();
    }

    @AutoValue
    /**
     * Holds a meta signal
     */
    public static abstract class SignalElement extends RichOutputElement {
        public abstract String signal();
    }
}
