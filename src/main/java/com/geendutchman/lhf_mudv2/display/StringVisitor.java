package com.geendutchman.lhf_mudv2.display;

import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.ExaminableElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.NestedElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.SignalElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.StringElement;
import com.geendutchman.lhf_mudv2.display.RichOutputElement.TaggableElement;

public class StringVisitor implements RichOutputElementVisitor {
    private final StringBuilder builder;

    private StringVisitor() {
        this.builder = new StringBuilder();
    }

    @Override
    public String toString() {
        return this.builder.toString();
    }

    public static String buildString(final RichOutput output) {
        final NestedElement nested = new NestedElement(output);
        final StringVisitor myself = new StringVisitor();
        myself.visit(nested);
        return myself.built();
    }

    public String built() {
        return this.builder.toString();
    }

    @Override
    public void visit(final StringElement element) {
        this.builder.append(element.charSequence());
    }

    @Override
    public void visit(final TaggableElement taggable) {
        this.builder.append(taggable.taggable().content());
    }

    @Override
    public void visit(final ExaminableElement examinable) {
        final BasicExaminable examined = examinable.examinable();
        builder.append(examined.name()).append(":\n");
        if (!examined.content().equals(examined.name().toString())) {
            builder.append("\t").append(examined.content()).append("\n");
        }
        if (examined.description().isPresent()) {
            String done = StringVisitor.buildString(examined.description().get());
            for (final String line : done.split("\\r?\\n")) {
                builder.append("\t").append(line).append("\n");
            }
        }
    }

    @Override
    public void visit(final SignalElement signal) {
    }

    @Override
    public void visit(final NestedElement nested) {
        final RichOutput output = nested.nested();
        output.sequenceName().ifPresent(seqName -> builder.append(seqName).append(":\n\t"));
        for (final RichOutputElement element : output.elements()) {
            if (element == null) {
                continue;
            }
            StringVisitor childVisitor = new StringVisitor();
            switch (element) {
            case RichOutputElement.StringElement se -> {
                childVisitor.visit(se);
            }
            case RichOutputElement.TaggableElement te -> {
                childVisitor.visit(te);
            }
            case RichOutputElement.ExaminableElement ee -> {
                childVisitor.visit(ee);
            }
            case RichOutputElement.SignalElement se -> {
                childVisitor.visit(se);
            }
            case RichOutputElement.NestedElement ne -> {
                childVisitor.visit(ne);
            }
            case null -> {
            }
            default -> {
            }

            }
            String child = childVisitor.built();
            final String[] splitten = child.split("\r?\n");
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
                if (output.elementSeparator().isPresent()) {
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

            }
        }
    }

}
