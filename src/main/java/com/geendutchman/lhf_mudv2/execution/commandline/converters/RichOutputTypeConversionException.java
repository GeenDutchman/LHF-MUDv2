package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import com.geendutchman.lhf_mudv2.display.RichOutput;

import picocli.CommandLine.TypeConversionException;

public class RichOutputTypeConversionException extends TypeConversionException {
    private final RichOutput output;

    public RichOutputTypeConversionException(RichOutput out) {
        super(out.printIt());
        this.output = out;
    }

    public RichOutputTypeConversionException(String out) {
        super(out);
        this.output = RichOutput.builder().setSequenceName("Issue Converting Type").addString(out).build();
    }

    public RichOutput getOutput() {
        return this.output;
    }

}