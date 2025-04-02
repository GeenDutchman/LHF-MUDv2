package com.geendutchman.lhf_mudv2.events;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

public class EventTest {
    @Test
    void testFlux() {
        TestPublisher<String> pub = TestPublisher.<String>createCold().emit("Foo", "Bar");
        StepVerifier.create(pub.flux()).expectSubscription().expectNext("Foo").expectNext("Bar").expectComplete()
                .verify();
    }
}
