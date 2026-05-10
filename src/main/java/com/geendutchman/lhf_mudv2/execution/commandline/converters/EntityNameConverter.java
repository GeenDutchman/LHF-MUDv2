package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableSet;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

abstract class EntityNameConverter<T extends Entity> implements ITypeConverter<T> {

    private final MessageContext context;

    public EntityNameConverter(MessageContext ctx) {
        if (ctx != null) {
            this.context = ctx;
        } else {
            throw new IllegalArgumentException("Context must not be null");
        }
    }

    public abstract String entityTypeName();

    public MessageContext ctx() {
        return this.context;
    }

    protected abstract T singleContextCheck(final String trimmed);

    protected abstract ImmutableSet<T> manyContextCheck(final String trimmed);

    @Override
    public T convert(String value) throws Exception {
        if (value == null || value.trim().length() < 3) {
            throw new TypeConversionException(
                    String.format("A %s name must be at least three letters long", this.entityTypeName()));
        }
        final String trimmed = value.trim();
        final T found = this.singleContextCheck(trimmed);
        if (found != null && found.name().toString().equalsIgnoreCase(trimmed)) {
            return found;
        }
        final ImmutableSet<T> many = this.manyContextCheck(trimmed);
        if (many == null || many.isEmpty()) {
            throw new TypeConversionException(
                    String.format("No %s found with a name starting with '%s'", this.entityTypeName(), trimmed));
        }
        if (many.size() == 1) {
            return many.asList().getFirst();
        } else {
            final RichOutput.Builder out = RichOutput.builder()
                    .setSequenceName(String.format("Specific %s Not Found", this.entityTypeName()))
                    .addString("Your search for a").addString(this.entityTypeName())
                    .addString("with a name that starts with").addString(String.format("\"%s\"", trimmed))
                    .addString("was not specific enough.");
            final RichOutput.Builder sub = RichOutput.builder().setSequenceName("Potential Matches")
                    .setElementSeparator(Optional.of(RichOutputElement.ofString("\n - ")));
            many.stream().limit(10).forEach(c -> sub.addTaggable(c));
            out.addOutput(sub.build());
            throw new RichOutputTypeConversionException(out.build());
        }
    }
}