package com.geendutchman.lhf_mudv2.display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.NonPrintableStyle;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

public record YAMLFormatter(Environment environment, StackTracePrinter stackTracePrinter, ContextPairs contextPairs,
        ThrowableProxyConverter throwableProxyConverter, StructuredLoggingJsonMembersCustomizer<?> customizer)
        implements StructuredLogFormatter<ILoggingEvent> {

    private static boolean USE_CONVERTER = false;

    public YAMLFormatter {
    }

    @Override
    public String format(ILoggingEvent event) {
        DumperOptions dumpOpts = new DumperOptions();
        dumpOpts.setNonPrintableStyle(NonPrintableStyle.ESCAPE);
        final Yaml yaml = new Yaml(dumpOpts);
        final Map<String, Object> collected = this.eventAsMap(event);
        final String asString = yaml.dump(List.of(collected));
        return asString;
    }

    protected Map<String, Object> eventAsMap(ILoggingEvent event) {
        final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

        data.put("loggerName", event.getLoggerName());
        data.put("sequenceNumber", event.getSequenceNumber());
        data.put("instant", event.getInstant().toString());
        data.put("level", event.getLevel().toString());
        data.put("message", event.getFormattedMessage());

        final LinkedHashMap<String, String> process = new LinkedHashMap<>(2);
        process.put("threadName", event.getThreadName());
        data.put("process", process);

        final Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && mdc.size() > 0) {
            data.put("mdc", mdc);
        }

        final List<KeyValuePair> kv = event.getKeyValuePairs();
        if (kv != null && kv.size() > 0) {
            final LinkedHashMap<String, Object> nextKv = new LinkedHashMap<>(kv.size());
            for (KeyValuePair pair : kv) {
                if (pair == null) {
                    continue;
                }
                nextKv.put(pair.key, pair.value);
            }
            data.put("key-values", nextKv);
        }

        final List<Marker> markers = event.getMarkerList();
        if (markers != null && markers.size() > 0) {
            data.put("markers", markers);
        }

        if (this.throwableProxyConverter != null && USE_CONVERTER) {
            List<Object> thrown = cleanString(this.throwableProxyConverter.convert(event));
            if (thrown != null && thrown.size() > 0) {
                data.put("thrown", thrown);
            }
        } else {
            final IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                data.put("exception", this.forThrowable(throwableProxy));
            }
        }

        return data;
    }

    protected List<Object> cleanString(String toClean) {
        if (toClean == null) {
            return null;
        }
        String noAnsi = toClean.replaceAll("\u001B\\[[;\\d]*[mK]", "");
        String[] splitten = noAnsi.split("\\r?\\n");
        ArrayList<Object> things = new ArrayList<>();

        for (String element : splitten) {
            things.add(element.replaceAll("\\t", " - "));
        }
        return things;
    }

    protected Map<String, Object> forThrowable(IThrowableProxy proxy) {
        final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        if (proxy == null) {
            return data;
        }

        data.put("throwMessage", cleanString(proxy.getMessage()));
        data.put("errorType", proxy.getClassName());

        final IThrowableProxy cause = proxy.getCause();
        if (cause != null) {
            Map<String, Object> caused = forThrowable(cause);
            if (caused != null && caused.size() > 0) {
                data.put("cause", caused);
            }
        }

        final IThrowableProxy[] suppressed = proxy.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
            final ArrayList<Map<String, Object>> suplist = new ArrayList<>(suppressed.length);
            for (IThrowableProxy subProxy : suppressed) {
                if (subProxy == null) {
                    continue;
                }
                suplist.add(forThrowable(subProxy));
            }
            data.put("suppressed", suplist);
        }

        final StackTraceElementProxy[] stackTrace = proxy.getStackTraceElementProxyArray();
        if (stackTrace != null && stackTrace.length > 0) {
            final ArrayList<Object> stack = new ArrayList<>(stackTrace.length);
            for (StackTraceElementProxy trace : stackTrace) {
                if (trace == null) {
                    continue;
                }
                stack.add(cleanString(trace.toString()));
            }
            data.put("throwTrace", stack);
        }

        return data;
    }

}
