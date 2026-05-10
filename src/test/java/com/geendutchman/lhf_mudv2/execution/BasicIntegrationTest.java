package com.geendutchman.lhf_mudv2.execution;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
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
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.controllers.TestCreatureController;
import com.geendutchman.lhf_mudv2.junction.LogWriter;
import com.github.f4b6a3.tsid.Tsid;
import com.google.common.truth.Truth;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import picocli.CommandLine;

@SpringBootTest
public class BasicIntegrationTest {

    @Autowired
    MessageBus bus;
    @Autowired
    CreatureRepository creatureRepository;
    @Autowired
    BiFunction<MessageBus, MessageContext, CommandLine> generator;
    @Autowired
    Duration duration;

    private Stream<DynamicNode> expandCLI(Collection<CommandLine> lines) {
        if (lines == null) {
            return Stream.of();
        }
        return lines.stream().filter(line -> line != null).map(line -> {
            DynamicNode node = DynamicContainer.dynamicContainer(line.getCommandName(),
                    Stream.concat(Stream.of(DynamicTest.dynamicTest(line.getCommandName(), () -> {
                        line.usage(line.getOut());
                    })), expandCLI(line.getSubcommands().values())));
            return node;
        });
    }

    @TestFactory
    Stream<DynamicNode> testGenerate() {
        Logger logger = LoggerFactory.getLogger(getClass());
        MessageContext ctx = MessageContext.builder().setSenderId(IEntityID.BLANK_ID)
                .setDestinationId(IEntityID.BLANK_ID).build();
        CommandLine built = generator.apply(bus, ctx);
        built.setOut(new PrintWriter(new LogWriter(logger)));
        built.setErr(new PrintWriter(new LogWriter(logger, Level.ERROR)));
        built.usage(built.getOut());
        return expandCLI(Set.of(built));

    }

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

        TestCreatureController creatureController = new TestCreatureController(bus, generator) {

            @Override
            protected Name name() {
                return new Examinable.Name("Look Tester");
            }
        };
        creatureController.setRoomSeenHook((context, creature, event) -> {
            if (event != null && event.toString().contains("Statue Bust")) {
                canProceed.countDown();
            }
            return false;
        });

        bus.registerProcessor(creatureController);
        bus.registerEntity(tester, creatureController.messageProcessorID());

        room.applyDelta(Room.Delta.ofCreatureToAdd(tester));

        MessageProcessingResult sendResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.creatureID()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(), "see", false));
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

        MessageContext context = MessageContext.builder().setSenderId(tester.creatureID())
                .setDestinationId(tester.identifier()).build();

        MessageProcessingResult takeResult = bus.send(context,
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(), "take \"Statue Bust\"", false));
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
        final Logger logger = LoggerFactory.getLogger(getClass());

        CyclicBarrier canProceed = new CyclicBarrier(2);

        TestCreatureController creatureController = new TestCreatureController(bus, generator) {

            @Override
            protected Name name() {
                return new Examinable.Name("Test Going");
            }
        };
        creatureController.setRoomSeenHook((context, creature, event) -> {
            try {
                logger.info(event.description().printIt());
                canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
                // canProceed.await();
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                logger.error("Testing had a problem", e);
                throw new UncheckedTimeoutException(e);
            }
            return false;
        });
        creatureController.setPlainEventHook((context, creature, event) -> {
            try {
                logger.info(event.description().printIt());
                canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                logger.error("Testing had a problem", e);
                throw new UncheckedTimeoutException(e);
            }
            return false;
        });

        bus.registerProcessor(creatureController);
        bus.registerEntity(tester, creatureController.messageProcessorID());

        // do the four cardinal directions

        MessageProcessingResult goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                        String.format("go %s", Directions.NORTH.name()), false));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomB.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomB).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                        String.format("go %s", Directions.EAST.name()), false));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomC.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomC).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                        String.format("go %s", Directions.SOUTH.name()), false));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomD.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomD).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(), String.format("go %s", Directions.WEST),
                        false));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(), String.format("go %s", Directions.UP.name()),
                        false));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomZ.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomZ).creatures().contains(tester);

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                        String.format("go %s", Directions.DOWN.name()), false));
        Truth.assertThat(goResult).isEqualTo(MessageProcessingResult.HANDLED);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);

        // Only Monsters can go from roomA east to roomD

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                        String.format("go %s", Directions.EAST.name()), false));
        Truth.assertThat(goResult).isInstanceOf(MessageProcessingResult.Failed.class);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);
        Truth.assertAbout(RoomSubject.rooms()).that(roomD).creatures().doesNotContain(tester);

        // And south to a null room does not work

        canProceed.reset();
        goResult = bus.send(
                MessageContext.builder().setSenderId(tester.creatureID()).setDestinationId(tester.identifier()).build(),
                new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                        String.format("go %s", Directions.SOUTH.name()), false));
        Truth.assertThat(goResult).isInstanceOf(MessageProcessingResult.Failed.class);

        canProceed.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        Truth.assertAbout(CreatureSubject.creatures()).that(tester).locale().hasValue(roomA.roomID());
        Truth.assertAbout(RoomSubject.rooms()).that(roomA).creatures().contains(tester);

    }

}
