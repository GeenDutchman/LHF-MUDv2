package com.geendutchman.lhf_mudv2.execution;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomBuilderFactory;
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
        Creature tester = CreatureBuilderFactory.builder().setName("Tester").setHealth(10).scores4d6DropLowest()
                .setFaction(Faction.NPC).build(creatureBuilderFactory);

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

}
