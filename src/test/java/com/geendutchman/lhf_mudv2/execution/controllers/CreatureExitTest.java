package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.creature.CreatureContainerSubject;
import com.geendutchman.lhf_mudv2.entities.creature.CreatureSubject;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.NameGenerationStrategy;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.google.common.truth.Truth;

@SpringBootTest
public class CreatureExitTest extends ItemExitTest {

    @Autowired
    protected final CreatureController creatureController;

    @Autowired
    protected final CreatureRepository creatureRepository;

    @Autowired
    protected final CreatureBuilderFactory creatureBuilderFactory;

    protected Creature creature;

    @Autowired
    public CreatureExitTest(ItemController itemController, ItemRepository itemRepository,
            ItemBuilderFactory itemBuilderFactory, CreatureController creatureController,
            CreatureRepository creatureRepository, CreatureBuilderFactory creatureBuilderFactory) {
        super(itemController, itemRepository, itemBuilderFactory);
        this.creatureController = creatureController;
        this.creatureRepository = creatureRepository;
        this.creatureBuilderFactory = creatureBuilderFactory;
    }

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        this.creature = CreatureBuilderFactory.builder()
                .setNameGenerationStrategy(new NameGenerationStrategy(NameGenerationStrategy.Kind.PIN_LASTNAME,
                        Optional.ofNullable(new Examinable.Name("Tester"))))
                .setFaction(Faction.NPC).scores4d6DropLowest().setHealth(10).build(creatureBuilderFactory);
        this.creature.applyDelta(Creature.Delta.ofItemToAdd(item));
    }

    @Override
    @Test
    void testItemExit() {
        CreatureSubject.assertThat(creature).items().hasItem(item);
        super.testItemExit();
        CreatureSubject.assertThat(creature).items().doesNotHaveItem(item);
    }

    @Test
    void testCreatureExit() {
        CreatureContainerSubject.assertThat(creatureRepository).hasCreature(creature);
        final MessageContext context = MessageContext.builder().setSenderId(creature.creatureID())
                .setDestinationId(creature.creatureID()).build();
        final LHFCommand.LineCommand exitCommand = new LHFCommand.LineCommand(LHFCommand.idFactory.create(), "exit",
                false);
        final MessageProcessingResult result = creatureController.process(context, exitCommand);
        Truth.assertThat(result).isEqualTo(MessageProcessingResult.HANDLED);
        CreatureContainerSubject.assertThat(creatureRepository).doesNotHaveCreature(creature);
    }

}
