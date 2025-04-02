package com.geendutchman.lhf_mudv2.events;

import org.reactivestreams.Subscription;

import reactor.core.publisher.BaseSubscriber;

public class EventSubscriber extends BaseSubscriber<Event> {

    @Override
    public void hookOnSubscribe(Subscription subscription) {
        System.out.println("Subscribed");
        request(1);
    }

    @Override
    public void hookOnNext(Event value) {
        System.out.println(value);
        request(1);
    }
}
