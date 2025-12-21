package com.geendutchman.lhf_mudv2.execution;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.creature.CreatureSubject;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.NameGenerationStrategy;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.RoomSubject;
import com.geendutchman.lhf_mudv2.execution.Event.RoomSeenEvent;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.controllers.CreatureController;
import com.geendutchman.lhf_mudv2.execution.controllers.TestCreatureController;
import com.google.common.truth.Truth;

@SpringBootTest
public class BasicIntegrationTest {

    @Autowired
    MessageBus bus;
    @Autowired
    CreatureRepository creatureRepository;
    @Autowired
    Duration duration;

    @Test
    void testLook(@Autowired RoomBuilderFactory roomBuilderFactory,
            @Autowired CreatureBuilderFactory creatureBuilderFactory) throws InterruptedException {
        Room room = RoomBuilderFactory.builder().setName("Test Room")
                .addItem(ItemBuilderFactory.builder().setName("Statue Bust").lock()).addCreature(CreatureBuilderFactory
                        .builder().setName("EyeCandy").setHealth(10).scores4d6DropLowest().setFaction(Faction.NPC))
                .build(roomBuilderFactory);
        Creature tester = CreatureBuilderFactory.builder()
                .setNameGenerationStrategy(NameGenerationStrategy.ofPinnedFirstname("Tester")).setHealth(10)
                .scores4d6DropLowest().setFaction(Faction.NPC).build(creatureBuilderFactory);

        CountDownLatch canProceed = new CountDownLatch(1);

        CreatureController creatureController = new TestCreatureController(bus, creatureRepository) {
            @Override
            protected void onRoomSeenEvent(MessageContext context, RoomSeenEvent event, Creature creature) {
                if (event != null && event.toString().contains("Statue Bust")) {
                    canProceed.countDown();
                }
                super.onRoomSeenEvent(context, event, creature);
            }
        };

        bus.registerProcessor(creatureController);
        bus.registerEntity(tester, creatureController.messageProcessorID());

        room.applyDelta(Room.Delta.ofCreatureToAdd(tester));

        MessageProcessingResult sendResult = creatureController.process(
                MessageContext.create(tester.creatureID(), tester.creatureID()),
                new UserCommand.SeeCommand(UserCommand.SeeCommand.idFactory.create(), Optional.empty()));
        Truth.assertThat(sendResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);

    }

    @Test
    void testTake(@Autowired RoomBuilderFactory roomBuilderFactory,
            @Autowired CreatureBuilderFactory creatureBuilderFactory) {
        Room room = RoomBuilderFactory.builder().setName("Test Room")
                .addItem(ItemBuilderFactory.builder().setName("Statue Bust").lock()).addCreature(CreatureBuilderFactory
                        .builder().setName("EyeCandy").setHealth(10).scores4d6DropLowest().setFaction(Faction.NPC))
                .build(roomBuilderFactory);
        Creature tester = CreatureBuilderFactory.builder()
                .setNameGenerationStrategy(NameGenerationStrategy.ofPinnedFirstname("Tester")).setHealth(10)
                .scores4d6DropLowest().setFaction(Faction.NPC).build(creatureBuilderFactory);

        room.applyDelta(Room.Delta.ofCreatureToAdd(tester));

        RoomSubject.assertThat(room).items().queryOne(ItemQuery.builder().setDisplayName("Statue Bust").build())
                .isPresent();

        MessageContext context = MessageContext.create(tester.creatureID(), tester.identifier());

        MessageProcessingResult takeResult = bus.send(context,
                new UserCommand.TakeCommand(UserCommand.TakeCommand.idFactory.create(), "Statue Bust"));
        Truth.assertThat(takeResult).isEqualTo(MessageProcessingResult.HANDLED);

        Assertions.assertTimeout(duration, () -> {
            RoomSubject.assertThat(room).items().queryOne(ItemQuery.builder().setDisplayName("Statue Bust").build())
                    .isEmpty();
            CreatureSubject.assertThat(tester).items()
                    .queryOne(ItemQuery.builder().setDisplayName("Statue Bust").build()).isPresent();
        });

    }

}
