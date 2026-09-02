package com.geendutchman.lhf_mudv2.junction.common;

import java.io.Reader;
import java.io.Writer;
import java.util.Scanner;
import java.util.regex.Pattern;

public class StreamJunction {
    private final Reader input;
    private final Writer output;

    public StreamJunction(Reader input, Writer output) {
        this.input = input;
        this.output = output;
    }

    public void run() throws Exception {
        Pattern exiter = Pattern.compile("exit");
        try (final Scanner scanner = new Scanner(this.input)) {
            this.output.write("Initializing...\n");
            this.output.flush();
            while (scanner.hasNext()) {
                final String line = scanner.next();
                this.output.write(String.format("Recieved: '%s'\n", line));
                this.output.flush();
                if (exiter.asPredicate().test(line)) {
                    this.output.write("exit detected\n");
                    this.output.flush();
                    return;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            throw e; // just rethrow for now
        }
    }

}
