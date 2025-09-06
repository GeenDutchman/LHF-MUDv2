package com.geendutchman.lhf_mudv2.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.atLeastOnce;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.google.common.truth.Truth;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class EventBusTest {

    @Mock
    private Item observer;

    @Autowired
    private Duration timing;
    @Autowired
    private EventBus bus;
    @Autowired
    private ItemBuilderFactory itemFactory;

    private Duration testTransform() {
        return this.timing.plusSeconds(3);
    }

    @Test
    @Qualifier("virtualBus")
    public void testSeeItem() {
        System.out.println("I have started");
        Truth.assertThat(bus.listProcessors()).isNotEmpty();

        final Item sword = itemFactory.builder().setName("Sword").build();

        final ItemID id = ItemID.make("Observer");

        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(observer).processEvent(Mockito.any(), Mockito.any());
        Mockito.when(observer.processorURI()).thenReturn(id.uri());

        bus.register(observer);

        final Event event = Events.addressed().setDestination(sword.processorURI())
                .setReplyToSender(observer.processorURI()).seeEvent().build();

        bus.publish(event);

        final Duration transformed = this.testTransform();

        assertDoesNotThrow(() -> latch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        Truth.assertWithMessage("latch is zeroed out").that(latch.getCount()).isEqualTo(0);
        Mockito.verify(observer, atLeastOnce()).processEvent(Mockito.any(), Mockito.any());
        Mockito.verifyNoMoreInteractions(observer);
    }

    @Test
    @Qualifier("queuedEventBus")
    public void testQueuedSeeItem() {
        System.out.println("I have started");
        Truth.assertThat(bus.listProcessors()).isNotEmpty();

        final Item sword = itemFactory.builder().setName("Sword").build();

        final ItemID id = ItemID.make("Observer");

        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(observer).processEvent(Mockito.any(), Mockito.any());
        Mockito.when(observer.processorURI()).thenReturn(id.uri());

        bus.register(observer);

        final Event event = Events.addressed().setDestination(sword.processorURI())
                .setReplyToSender(observer.processorURI()).seeEvent().build();

        bus.publish(event);

        final Duration transformed = this.testTransform();

        assertDoesNotThrow(() -> latch.await(transformed.toNanos(), TimeUnit.NANOSECONDS));
        Truth.assertWithMessage("latch is zeroed out").that(latch.getCount()).isEqualTo(0);
        Mockito.verify(observer, atLeastOnce()).processEvent(Mockito.any(), Mockito.any());
        Mockito.verifyNoMoreInteractions(observer);
    }
}
