package com.geendutchman.lhf_mudv2.execution.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.RoomContainerSubject;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.entities.room.RoomSubject;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.google.common.truth.Truth;

@SpringBootTest
public class RoomExitTest extends CreatureExitTest {

    @Autowired
    protected final RoomController roomController;

    @Autowired
    protected final RoomRepository roomRepository;

    @Autowired
    protected final RoomBuilderFactory roomBuilderFactory;

    protected Room room;

    @Autowired
    public RoomExitTest(ItemController itemController, ItemRepository itemRepository,
            ItemBuilderFactory itemBuilderFactory, CreatureController creatureController,
            CreatureRepository creatureRepository, CreatureBuilderFactory creatureBuilderFactory,
            RoomController roomController, RoomRepository roomRepository, RoomBuilderFactory roomBuilderFactory) {
        super(itemController, itemRepository, itemBuilderFactory, creatureController, creatureRepository,
                creatureBuilderFactory);
        this.roomController = roomController;
        this.roomRepository = roomRepository;
        this.roomBuilderFactory = roomBuilderFactory;
    }

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        this.room = RoomBuilderFactory.builder().setName("Test Room").build(roomBuilderFactory);
        this.room.applyDelta(Room.Delta.ofCreatureToAdd(creature));
    }

    @Override
    @Test
    void testItemExit() {
        RoomSubject.assertThat(room).items().doesNotHaveItem(item); // the creature has the item
        super.testItemExit();
        RoomSubject.assertThat(room).items().doesNotHaveItem(item);
    }

    @Test
    void testRoomItemExit() {
        Item roomItem = ItemBuilderFactory.builder().setName("Floor Item").build(itemBuilderFactory);
        this.room.applyDelta(Room.Delta.ofItemToAdd(roomItem));
        RoomSubject.assertThat(room).items().hasItem(roomItem);
        final MessageContext context = MessageContext.builder().setSender(roomItem.itemID())
                .setDestination(roomItem.itemID()).build();
        final LHFCommand.LineCommand exitCommand = new LHFCommand.LineCommand(LHFCommand.idFactory.create(), "exit",
                false);

        final MessageProcessingResult result = itemController.process(context, exitCommand);
        Truth.assertThat(result).isEqualTo(MessageProcessingResult.HANDLED);
        RoomSubject.assertThat(room).items().doesNotHaveItem(roomItem);
    }

    @Override
    @Test
    void testCreatureExit() {
        RoomSubject.assertThat(room).creatures().hasCreature(creature);
        super.testCreatureExit();
        RoomSubject.assertThat(room).creatures().doesNotHaveCreature(creature);
    }

    @Test
    void testRoomExit() {
        RoomContainerSubject.assertThat(roomRepository).hasRoom(room);
        final MessageContext context = MessageContext.builder().setSender(room.roomID()).setDestination(room.roomID())
                .build();
        final LHFCommand.LineCommand exitCommand = new LHFCommand.LineCommand(LHFCommand.idFactory.create(), "exit",
                false);

        final MessageProcessingResult result = roomController.process(context, exitCommand);
        Truth.assertThat(result).isEqualTo(MessageProcessingResult.HANDLED);
        RoomContainerSubject.assertThat(roomRepository).doesNotHaveRoom(room);
    }

}
