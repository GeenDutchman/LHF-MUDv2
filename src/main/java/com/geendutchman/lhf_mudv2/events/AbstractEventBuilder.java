package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.util.function.Consumer;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.events.Event.BuildStep;
import com.geendutchman.lhf_mudv2.events.Event.DescriptionStep;
import com.geendutchman.lhf_mudv2.events.Event.PlainEventBuilder;
import com.geendutchman.lhf_mudv2.events.Event.RoutingDestinationStep;
import com.geendutchman.lhf_mudv2.events.Event.RoutingStep;
import com.geendutchman.lhf_mudv2.events.EventRouting.EventRoutingBuilder;

/**
 * An abstraction of a builder for an Event using common build steps
 * 
 * Should be package private!
 */
abstract non-sealed class AbstractEventBuilder
        implements BuildStep, RoutingDestinationStep, RoutingStep, DescriptionStep {
    protected abstract PlainEventBuilder plainBuilder();

    @Override
    public BuildStep setDestination(URI dest) {
        this.plainBuilder().setDestination(dest);
        return this;
    }

    @Override
    public BuildStep adjustRouting(Consumer<EventRoutingBuilder> adjuster) {
        this.plainBuilder().adjustRouting(adjuster);
        return this;
    }

    @Override
    public BuildStep setRouting(EventRouting routing) {
        this.plainBuilder().setRouting(routing);
        return this;
    }

    @Override
    public RoutingDestinationStep setSender(URI sender) {
        this.plainBuilder().setSender(sender);
        return this;
    }

    @Override
    public RoutingStep adjustDescription(Consumer<RichOutput.Builder> adjuster) {
        this.plainBuilder().adjustDescription(adjuster);
        return this;
    }

    @Override
    public RoutingStep setDescription(RichOutput richOutput) {
        this.plainBuilder().setDescription(richOutput);
        return this;
    }

    @Override
    public RoutingStep setShortDescription(String desc) {
        this.plainBuilder().setShortDescription(desc);
        return this;
    }

}