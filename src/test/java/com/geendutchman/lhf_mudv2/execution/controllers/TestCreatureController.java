package com.geendutchman.lhf_mudv2.execution.controllers;

import org.springframework.boot.test.context.TestComponent;

import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.execution.MessageBus;

@TestComponent
public class TestCreatureController extends CreatureController {

    public TestCreatureController(MessageBus bus, CreatureRepository repo) {
        super(bus, repo);
    }

}