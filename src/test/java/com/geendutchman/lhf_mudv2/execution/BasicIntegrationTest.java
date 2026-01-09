package com.geendutchman.lhf_mudv2.execution;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.Name;
import com.geendutchman.lhf_mudv2.entities.creature.CreatureSubject;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.NameGenerationStrategy;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.room.Directions;
import com.geendutchman.lhf_mudv2.entities.room.Doorway;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.RoomSubject;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomSeenEvent;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.controllers.CreatureController;
import com.geendutchman.lhf_mudv2.execution.controllers.TestCreatureController;
import com.github.f4b6a3.tsid.Tsid;
import com.google.common.truth.Truth;
import com.google.common.util.concurrent.UncheckedTimeoutException;

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

            @Override
            protected Name name() {
                return new Examinable.Name("Look Tester");
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

    @Test
    void testGo(@Autowired RoomBuilderFactory roomBuilderFactory,
            @Autowired CreatureBuilderFactory creatureBuilderFactory)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        Room roomA = RoomBuilderFactory.builder().setName("roomA").build(roomBuilderFactory);
        Room roomB = RoomBuilderFactory.builder().setName("roomB").build(roomBuilderFactory);
        Room roomC = RoomBuilderFactory.builder().setName("roomC").build(roomBuilderFactory);
        Room roomD = RoomBuilderFactory.builder().setName("roomD").build(roomBuilderFactory);
        Room roomZ = RoomBuilderFactory.builder().setName("roomZ").build(roomBuilderFactory);

        roomBuilderFactory.dualConnect(roomA.roomID(), roomB.roomID(), Directions.NORTH);
        roomBuilderFactory.dualConnect(roomB.roomID(), roomC.roomID(), Directions.EAST);
        roomBuilderFactory.dualConnect(roomC.roomID(), roomD.roomID(), Directions.SOUTH);
        roomBuilderFactory.dualConnect(roomD.roomID(), roomA.roomID(), Directions.WEST);
        roomBuilderFactory.dualConnect(roomA.roomID(), roomZ.roomID(), Directions.UP);

        RoomID nullId = new RoomID(
                new IEntityID.EntityID(Room.RoomID.ENTITY_CLASS_ROOM, new Examinable.Name("null"), Tsid.fast()));
        roomA.applyDelta(
                new Room.Delta.AddDoorway(Directions.SOUTH, new Doorway(nullId, CreatureQuery.builder().build())));

        roomBuilderFactory.singleConnect(roomA.roomID(), roomD.roomID(), Directions.EAST,
                CreatureQuery.builder().setFaction(Faction.MONSTER).build());

        Creature tester = CreatureBuilderFactory.builder()
                .setNameGenerationStrategy(NameGenerationStrategy.ofPinnedFirstname("Tester")).setHealth(10)
                .scores4d6DropLowest().setFaction(Faction.NPC).build(creatureBuilderFactory);

        roomA.applyDelta(new Room.Delta.AddCreatureDelta(tester));

        CyclicBarrier canProceed = new CyclicBarrier(2);

        CreatureController creatureController = new TestCreatureController(bus, creatureRepository) {
            @Override
            protected void onRoomSeenEvent(MessageContext rcontext, RoomSeenEvent event, Creature creature) {
                super.onRoomSeenEvent(rcontext, event, creature);
                try {
                    logger.info(event.description().printIt());
                    canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
                    // canProceed.await();
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    logger.error("Testing had a problem", e);
                    throw new UncheckedTimeoutException(e);
                }
            }

            @Override
            protected void onPlainEvent(MessageContext context, PlainEvent event, Creature creature) {
                super.onPlainEvent(context, event, creature);
                try {
                    logger.info(event.description().printIt());
                    canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    logger.error("Testing had a problem", e);
                    throw new UncheckedTimeoutException(e);
                }
            }

            @Override
            protected Name name() {
                return new Examinable.Name("Test Going");
            }
        };

        bus.registerProcessor(creatureController);
        bus.registerEntity(tester, creatureController.messageProcessorID());

        // do the four cardinal directions

        MessageProcessingResult goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.NORTH.name()));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomB.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomB).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.EAST.name()));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomC.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomC).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.SOUTH.name()));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomD.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomD).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.WEST.name()));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.UP.name()));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomZ.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomZ).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.DOWN.name()));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);

        // Only Monsters can go from roomA east to roomD

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.EAST.name()));
        Truth.assertThat(goResult).isInstanceOf(MessageProcessingResult.Failed.class);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);
        Truth.assertAbout(RoomSubject.rooms()).that(roomD).creatures().doesNotContain(tester);

        // And south to a null room does not work

        canProceed.reset();
        goResult = bus.send(MessageContext.create(tester.creatureID(), tester.identifier()),
                new UserCommand.GoCommand(UserCommand.GoCommand.idFactory.create(), Directions.SOUTH.name()));
        Truth.assertThat(goResult).isInstanceOf(MessageProcessingResult.Failed.class);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);

    }

}
