package com.geendutchman.lhf_mudv2.execution;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomBuilderFactory;
import com.geendutchman.lhf_mudv2.execution.controllers.TestCreatureController;

@SpringBootTest
public class BasicIntegrationTest {

    @Autowired
    TestCreatureController creatureController;

    @Test
    @Autowired
    void testLook(MessageBus bus, RoomBuilderFactory roomBuilderFactory,
            CreatureBuilderFactory creatureBuilderFactory) {
        Room room = RoomBuilderFactory.builder().setName("Test Room")
                .addItem(ItemBuilderFactory.builder().setName("Statue Bust").lock())
                .addCreature(CreatureBuilderFactory.builder().setName("EyeCandy").setHealth(10).scores4d6DropLowest())
                .build(roomBuilderFactory);
        Creature tester = CreatureBuilderFactory.builder().setName("Tester").setHealth(10).scores4d6DropLowest()
                .build(creatureBuilderFactory);
        bus.registerEntity(tester, creatureController.messageProcessorID());

        room.applyDelta(Room.Delta.ofCreatureToAdd(tester));

    }

}
