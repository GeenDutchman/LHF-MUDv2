package com.geendutchman.lhf_mudv2.execution.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainerSubject;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.google.common.truth.Truth;

@SpringBootTest
public class ItemExitTest {

    @Autowired
    protected final ItemController itemController;

    @Autowired
    protected final ItemRepository itemRepository;

    @Autowired
    protected final ItemBuilderFactory itemBuilderFactory;

    protected Item item;

    @Autowired
    public ItemExitTest(ItemController itemController, ItemRepository itemRepository,
            ItemBuilderFactory itemBuilderFactory) {
        this.itemController = itemController;
        this.itemRepository = itemRepository;
        this.itemBuilderFactory = itemBuilderFactory;
    }

    @BeforeEach
    protected void setUp() {
        this.item = ItemBuilderFactory.builder().setName("Meti").build(this.itemBuilderFactory);
    }

    @Test
    void testItemExit() {
        ItemContainerSubject.assertThat(itemRepository).hasItem(item);
        final MessageContext context = MessageContext.create(item.itemID(), item.itemID());
        final UserCommand.ExitCommand exitCommand = new UserCommand.ExitCommand(
                UserCommand.ExitCommand.idFactory.create());
        final MessageProcessingResult result = itemController.process(context, exitCommand);
        Truth.assertThat(result).isEqualTo(MessageProcessingResult.HANDLED);
        ItemContainerSubject.assertThat(itemRepository).doesNotHaveItem(item);
    }

}
