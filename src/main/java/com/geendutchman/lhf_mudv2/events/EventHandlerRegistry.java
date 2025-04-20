package com.geendutchman.lhf_mudv2.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSortedSet;

@Component
public class EventHandlerRegistry {
    private final Map<Class<? extends Event>, EventHandler<? extends Event>> handlers = new ConcurrentHashMap<>();
    private final transient Logger logger = Logger.getLogger("EventHandlerRegistry");

    public <E extends Event> void register(EventHandler<E> handler) {
        if (handler == null) {
            throw new NullPointerException("cannot register a null handler");
        }
        this.logger.info(() -> String.format("Adding handler for %s called %s", handler.handledType().getName(),
                handler.getClass().getName()));
        this.handlers.put(handler.handledType(), handler);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> EventHandler<Event> getHandler(Class<E> eventType) {
        final List<Class<?>> supers = this.getAllSuperTypes(eventType);

        for (Class<?> parent : supers) {
            if (parent == null || !Event.class.isAssignableFrom(parent)) {
                continue;
            }
            EventHandler<?> handler = handlers.get(parent);
            if (handler != null) {
                return (EventHandler<Event>) handler;
            }

        }
        logger.warning(String.format("No handler found for event %s", eventType.getName()));
        return null;
    }

    public ImmutableSortedSet<String> listHandlers() {
        return ImmutableSortedSet.copyOf(handlers.keySet().stream().map(clazz -> clazz.getName()).toList());
    }

    private List<Class<?>> getAllSuperTypes(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(type);

        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (current == null || current == Object.class)
                continue;

            result.add(current);
            Class<?> superclass = current.getSuperclass();
            if (superclass != null)
                queue.add(superclass);
            Collections.addAll(queue, current.getInterfaces());
        }

        return new ArrayList<>(result);
    }

}
