package com.geendutchman.lhf_mudv2.execution;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessorID;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.truth.Truth;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
public class MessageBusTest {
    @Autowired
    List<MessageBus> busses;

    private final static TsidFactory tsidFactory = TsidFactory.newInstance1024(Math.abs("test".hashCode() % 1024));

    @ParameterizedTest
    @FieldSource("busses")
    void testPublish(MessageBus bus) {
        Truth.assertThat(bus).isNotNull();
        MessageProcessor first = Mockito.mock();
        MessageProcessorID firstID = MessageProcessorID.nextID();
        Mockito.when(first.messageProcessorID()).thenReturn(firstID);
        IEntityID entity = new IEntityID.EntityID(new Taggable.Tag(bus.getClass().getSimpleName()),
                new Examinable.Name("events"), tsidFactory.create());
        bus.registerProcessor(first);
        bus.registerEntity(entity, firstID);

        Event event = Event.PlainEvent.asDescribed(RichOutput.builder().addString("I have coconuts").build());
        MessageContext context = MessageContext.create(entity, entity);
        bus.publish(context, event);

        Mockito.verify(first).process(context, event);
    }

    @ParameterizedTest
    @FieldSource("busses")
    void testSend(MessageBus bus) {
        Truth.assertThat(bus).isNotNull();
        MessageProcessor first = Mockito.mock();
        MessageProcessorID firstID = MessageProcessorID.nextID();
        Mockito.when(first.messageProcessorID()).thenReturn(firstID);
        IEntityID entity = new IEntityID.EntityID(new Taggable.Tag(bus.getClass().getSimpleName()),
                new Examinable.Name("commands"), tsidFactory.create());
        bus.registerProcessor(first);
        bus.registerEntity(entity, firstID);

        Command command = new UserCommand.SayCommand(UserCommand.SayCommand.idFactory.create(), "Hello there",
                Optional.empty());
        MessageContext context = MessageContext.create(entity, entity);
        bus.send(context, command);

        Mockito.verify(first).process(context, command);
    }
}
