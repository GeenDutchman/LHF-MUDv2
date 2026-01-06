package com.geendutchman.lhf_mudv2.display;

import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.Yaml;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

public class YAMLFormatter implements StructuredLogFormatter<ILoggingEvent> {

    public YAMLFormatter(Environment environment, StackTracePrinter stackTracePrinter, ContextPairs contextPairs,
            ThrowableProxyConverter throwableProxyConverter, StructuredLoggingJsonMembersCustomizer<?> customizer) {

    }

    @Override
    public String format(ILoggingEvent event) {
        final Yaml yaml = new Yaml();
        final LinkedHashMap<String, Object> collected = this.eventAsMap(event);
        final String asString = yaml.dump(List.of(collected));
        return asString;
    }

    protected LinkedHashMap<String, Object> eventAsMap(ILoggingEvent event) {
        final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("loggerName", event.getLoggerName());
        data.put("sequenceNumber", event.getSequenceNumber());
        data.put("instant", event.getInstant().toString());
        data.put("level", event.getLevel().toString());
        data.put("message", event.getFormattedMessage());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            data.put("exception", this.forThrowable(throwableProxy));
        }
        final LinkedHashMap<String, String> process = new LinkedHashMap<>();
        process.put("threadName", event.getThreadName());
        data.put("process", process);
        data.put("mdc", event.getMDCPropertyMap());
        data.put("key-values", event.getKeyValuePairs());
        data.put("markers", event.getMarkerList());

        return data;
    }

    protected LinkedHashMap<String, Object> forThrowable(IThrowableProxy proxy) {
        final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("throwMessage", proxy.getMessage());
        data.put("errorType", proxy.getClassName());
        data.put("throwTrace", proxy.getStackTraceElementProxyArray());
        return data;
    }

}
