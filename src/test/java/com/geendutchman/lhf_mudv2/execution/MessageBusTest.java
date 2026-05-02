package com.geendutchman.lhf_mudv2.execution;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessorID;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.truth.Truth;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
public class MessageBusTest {
    @Autowired
    List<MessageBus> busses;

    @Autowired
    private Duration timing;

    private final static TsidFactory tsidFactory = TsidFactory.newInstance1024(Math.abs("test".hashCode() % 1024));

    @ParameterizedTest
    @FieldSource("busses")
    void testPublish(MessageBus bus) {
        Truth.assertThat(bus).isNotNull();
        MessageProcessor first = Mockito.mock();
        MessageProcessorID firstID = MessageProcessorID.nextID(new Examinable.Name("testPublish"));
        Mockito.when(first.messageProcessorID()).thenReturn(firstID);
        IEntityID entity = new IEntityID.EntityID(new Taggable.Tag(bus.getClass().getSimpleName()),
                new Examinable.Name("events"), tsidFactory.create());
        bus.registerProcessor(first);
        bus.registerEntity(entity, firstID);

        Event event = Event.PlainEvent.asDescribed(RichOutput.builder().addString("I have coconuts").build());
        MessageContext context = MessageContext.builder().setSender(entity).setDestination(entity).build();
        MessageProcessingResult result = bus.publish(context, event);
        Truth.assertThat(result).isEqualTo(MessageProcessingResult.HANDLED);

        Mockito.verify(first, Mockito.timeout(timing.toMillis())).process(context, event);
    }

    @ParameterizedTest
    @FieldSource("busses")
    void testSend(MessageBus bus) {
        Truth.assertThat(bus).isNotNull();
        MessageProcessor first = Mockito.mock();
        MessageProcessorID firstID = MessageProcessorID.nextID(new Examinable.Name("test send"));
        Mockito.when(first.messageProcessorID()).thenReturn(firstID);
        IEntityID entity = new IEntityID.EntityID(new Taggable.Tag(bus.getClass().getSimpleName()),
                new Examinable.Name("commands"), tsidFactory.create());
        bus.registerProcessor(first);
        bus.registerEntity(entity, firstID);

        LHFCommand command = new LHFCommand.LineCommand(LHFCommand.idFactory.create(), "say \"Hello there\"", false);
        MessageContext context = MessageContext.builder().setSender(entity).setDestination(entity).build();
        bus.send(context, command);

        Mockito.verify(first, Mockito.timeout(timing.toMillis())).process(context, command);
    }

    @ParameterizedTest
    @FieldSource("busses")
    void testPublishRapidFire(MessageBus bus) {
        Truth.assertThat(bus).isNotNull();
        MessageProcessor first = Mockito.mock();
        MessageProcessorID firstID = MessageProcessorID.nextID(new Examinable.Name("test publish rapidfire"));
        final Logger firstLogger = LoggerFactory
                .getLogger(String.format("%s.%s", MessageProcessor.class.getClass().getName(), firstID));
        Mockito.when(first.messageProcessorID()).thenReturn(firstID);
        Mockito.when(first.process(Mockito.any(MessageContext.class), Mockito.any(Event.class)))
                .thenAnswer(new Answer<MessageProcessingResult>() {

                    @Override
                    public MessageProcessingResult answer(final InvocationOnMock invocation) throws Throwable {
                        final String invocationArgs = Arrays.toString(invocation.getArguments());
                        firstLogger.atInfo().log("{} invoked {}", bus.getClass().getSimpleName(), invocationArgs);
                        return MessageProcessingResult.HANDLED;
                    }

                });
        IEntityID entity = new IEntityID.EntityID(new Taggable.Tag(bus.getClass().getSimpleName()),
                new Examinable.Name("commands"), tsidFactory.create());
        bus.registerProcessor(first);
        bus.registerEntity(entity, firstID);

        MessageContext context = MessageContext.builder().setSender(entity).setDestination(entity).build();
        final int count = 30;
        for (int i = 0; i < count; i++) {
            Event event = Event.PlainEvent.asDescribed(RichOutput.builder()
                    .addString(String.format("I have %d coconuts for %s", i, bus.getClass().getSimpleName())).build());
            bus.publish(context, event);
        }
        Mockito.verify(first, Mockito.timeout(timing.toMillis()).times(count)).process(Mockito.any(),
                Mockito.any(Event.class));
    }
}
