package com.geendutchman.lhf_mudv2.execution.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;

public class ItemControllerTest {

    @Autowired
    protected ItemBuilderFactory itemFactory;

    @Autowired
    protected ItemController itemController;

    @Test
    void testProcess() {
        Item item = ItemBuilderFactory.builder().setName("Testing item").build(itemFactory);

    }

    @Test
    void testProcess2() {

    }
}
