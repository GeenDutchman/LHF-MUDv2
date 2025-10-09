package com.geendutchman.lhf_mudv2.display;

import com.geendutchman.lhf_mudv2.display.RichOutputElement.ExaminableElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.NestedElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.SignalElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.StringElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.TaggableElement;

public interface RichOutputElementVisitor {
    public final static class OutputBuilderConversionError extends RuntimeException {
        public OutputBuilderConversionError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void visit(final StringElement element);

    public void visit(final TaggableElement taggable);

    public void visit(final ExaminableElement examinable);

    public void visit(final SignalElement signal);

    public void visit(final NestedElement nested);
}
