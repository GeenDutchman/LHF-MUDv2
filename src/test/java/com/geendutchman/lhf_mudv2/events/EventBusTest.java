package com.geendutchman.lhf_mudv2.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.repository.QueryCodec;
import com.geendutchman.lhf_mudv2.events.EventProcessor.ProcessingResult;
import com.google.common.truth.Truth;

@SpringBootTest
// @ExtendWith(MockitoExtension.class)
public class EventBusTest {

    @Autowired
    private Duration timing;
    @Autowired
    private EventBus bus;
    @Autowired
    private ItemBuilderFactory itemFactory;
    @Autowired
    private QueryCodec.Factory queryFactory;

    private Duration testTransform() {
        return this.timing.plusSeconds(3);
    }

    @Test
    @Qualifier("virtualBus")
    public void testSeeItem() {
        System.out.println("I have started");
        Truth.assertThat(bus.listProcessors()).isNotEmpty();

        CountDownLatch latch = new CountDownLatch(1);
        final Item sword = ItemBuilderFactory.builder().setName("Sword").build(itemFactory);
        final Item observer = ItemBuilderFactory.builder().setName("Observer").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            System.out.println(event);
            latch.countDown();
            return new ProcessingResult.Handled();
        }).build(itemFactory);

        final Event event = Event.ItemSeenEvent.builder().setItem(sword)
                .adjustDescription(output -> output.addString("Something was seen?")).setSender(sword.processorURI())
                .setDestination(observer.processorURI()).build();

        bus.publish(event);

        final Duration transformed = this.testTransform();

        assertDoesNotThrow(() -> latch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        Truth.assertWithMessage("latch is zeroed out").that(latch.getCount()).isEqualTo(0);
    }

    @Test
    @Qualifier("queuedEventBus")
    public void testQueuedSeeItem() {
        System.out.println("I have started");
        Truth.assertThat(bus.listProcessors()).isNotEmpty();

        CountDownLatch latch = new CountDownLatch(1);
        final Item sword = ItemBuilderFactory.builder().setName("Sword").build(itemFactory);
        final Item observer = ItemBuilderFactory.builder().setName("Observer").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            latch.countDown();
            return new ProcessingResult.Handled();
        }).build(itemFactory);

        final Event event = Event.ItemSeenEvent.builder().setItem(sword)
                .adjustDescription(output -> output.addString("Something was seen?")).setSender(sword.processorURI())
                .setDestination(observer.processorURI()).build();

        bus.publish(event);

        final Duration transformed = this.testTransform();

        assertDoesNotThrow(() -> latch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        Truth.assertWithMessage("latch is zeroed out").that(latch.getCount()).isEqualTo(0);
    }

    @Test
    public void testBroadcast() {
        CountDownLatch talkerLatch = new CountDownLatch(1);
        CountDownLatch hearerLatch = new CountDownLatch(1);
        CountDownLatch observerLatch = new CountDownLatch(1);

        final Item talker = ItemBuilderFactory.builder().setName("talker").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            if (event.description().printIt().contains("talker")) {
                talkerLatch.countDown();
                return new ProcessingResult.Handled();
            }
            return new ProcessingResult.Unhandled();
        }).build(itemFactory);

        final Item hearer = ItemBuilderFactory.builder().setName("hearer").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            if (event.description().printIt().contains("hearer")) {
                hearerLatch.countDown();
                return new ProcessingResult.Handled();
            }
            return new ProcessingResult.Unhandled();
        }).build(itemFactory);

        ItemBuilderFactory.builder().setName("observer").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            if (event.description().printIt().contains("hearer")) {
                observerLatch.countDown();
                return new ProcessingResult.Handled();
            }
            return new ProcessingResult.Unhandled();
        }).build(itemFactory);

        final Event event = Event.SpokenEvent.builder().whoSpeaks(talker).whoListens(hearer)
                .setMessage(RichOutput.builder().addString("I say unto thee, listen!").build())
                .setSender(talker.processorURI())
                .setDestination(UriComponentsBuilder.fromPath("/items").build().toUri()).build();
        System.out.println(event.description().printIt());
        Truth.assertThat(event.description().printIt()).contains(talker.name());
        Truth.assertThat(event.description().printIt()).contains(hearer.name());

        bus.publish(event);

        final Duration transformed = this.testTransform();

        assertDoesNotThrow(() -> talkerLatch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        assertDoesNotThrow(() -> hearerLatch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        assertDoesNotThrow(() -> observerLatch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));

        Truth.assertWithMessage("talker must hear").that(talkerLatch.getCount()).isEqualTo(0);
        Truth.assertWithMessage("hearer must listen").that(hearerLatch.getCount()).isEqualTo(0);
        Truth.assertWithMessage("observer must observer").that(observerLatch.getCount()).isEqualTo(0);

    }

    @Test
    public void testQueriedBroadcast() {
        CountDownLatch talkerLatch = new CountDownLatch(1);
        CountDownLatch hearerLatch = new CountDownLatch(1);
        CountDownLatch dumbdumb = new CountDownLatch(1);

        final Item talker = ItemBuilderFactory.builder().setName("talker").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            if (event.description().printIt().contains("talker")) {
                talkerLatch.countDown();
                return new ProcessingResult.Handled();
            }
            return new ProcessingResult.Unhandled();
        }).build(itemFactory);

        final Item hearer = ItemBuilderFactory.builder().setName("hearer").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            if (event.description().printIt().contains("hearer")) {
                hearerLatch.countDown();
                return new ProcessingResult.Handled();
            }
            return new ProcessingResult.Unhandled();
        }).build(itemFactory);

        ItemBuilderFactory.builder().setName("dumbdumb").setEventFunction((event, processor) -> {
            Truth.assertWithMessage("event should not be null").that(event).isNotNull();
            Truth.assertWithMessage("bus should not be null").that(bus).isNotNull();
            Truth.assertWithMessage("processor should not be null").that(processor).isNotNull();
            if (event.description().printIt().contains("hearer")) {
                dumbdumb.countDown();
                return new ProcessingResult.Handled();
            }
            return new ProcessingResult.Unhandled();
        }).build(itemFactory);

        ItemQuery query = ItemQuery.builder().setDisplayNamePattern(".*er.*").build();

        final Event event = Event.SpokenEvent.builder().whoSpeaks(talker).whoListens(hearer)
                .setMessage(RichOutput.builder().addPolymorphic("I say unto thee, listen!").build())
                .setSender(talker.processorURI()).setDestination(queryFactory.defaultEntityQueryCodec()
                        .toURI(query, UriComponentsBuilder.fromPath("/items")).build().toUri())
                .build();
        System.out.println(event.description().printIt());
        Truth.assertThat(event.description().printIt()).contains(talker.name());
        Truth.assertThat(event.description().printIt()).contains(hearer.name());

        bus.publish(event);

        final Duration transformed = this.testTransform();

        assertDoesNotThrow(() -> talkerLatch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        assertDoesNotThrow(() -> hearerLatch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        Truth.assertThat(assertDoesNotThrow(() -> dumbdumb.await(transformed.toNanos(), TimeUnit.NANOSECONDS)))
                .isFalse();

        Truth.assertWithMessage("talker must hear").that(talkerLatch.getCount()).isEqualTo(0);
        Truth.assertWithMessage("hearer must listen").that(hearerLatch.getCount()).isEqualTo(0);
        Truth.assertWithMessage("dumdumb must not hear").that(dumbdumb.getCount()).isEqualTo(1);

    }
}
