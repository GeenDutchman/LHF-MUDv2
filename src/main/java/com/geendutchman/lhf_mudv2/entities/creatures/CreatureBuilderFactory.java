package com.geendutchman.lhf_mudv2.entities.creatures;

import java.io.Serializable;
import java.net.URI;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.dice.D6Set;
import com.geendutchman.lhf_mudv2.dice.DiceSet;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.Name;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@Component
public final class CreatureBuilderFactory implements EventProcessor {
    private final CreatureRepository repository;
    private final EventBus bus;
    private final char renameAttempts = 7;
    private final URI processorURI;

    private final ConcurrentSkipListSet<Examinable.Name> namesRegister = new ConcurrentSkipListSet<>();

    private final static ImmutableList<Examinable.Name> firstNames = ImmutableSet.<String>builder()
            .add("Zanzibar", "Glocken", "Lorc", "Hammurabi", "Aztec", "Neolithic", "Hardy", "Joe", "Huroc", "Charlie",
                    "Teton", "Adzegrinder", "Heelton", "Bandit", "Chili", "Nibs", "Treebeard", "Tararaniglesprout",
                    "Edgelord", "Gobgob", "Child", "Tartarus", "Bravo", "Yzma", "Qurokrok", "Sophie", "Sofi", "Zuroc",
                    "Narcnarc the Great", "Corpus", "Dokkun the", "Carnival", "Iiiiiii", "Wenwhen", "Wenwihen",
                    "Asdiff", "Yelnats", "Stanley", "Volv Vlov", "George Shenanigans", "Hirn", "Nirn", "Del",
                    "LLedehtinremraf", "Hork", "Benjamin", "Trevor", "Tony", "Ashley", "Ozzy", "Shadow")
            .build().stream().map(name -> new Name(name)).collect(ImmutableList.toImmutableList());

    private final static ImmutableList<Examinable.Name> lastNames = ImmutableSet.<String>builder()
            .add("Swordswallower", "Yelnats", "Stanley", "The Terrible", "The Tattletale", "Iiiiii", "Koshima", "Smith",
                    "Doe", "Listless", "Restless", "Barnraiser", "Barddestroyer", "Patriot", "Wang", "Lee", "Zeusson",
                    "Suessson", "Heckenschutter", "Trapp", "Von Varr", "Pon Farr", "Saltlicker", "Patronym", "Matrinym",
                    "Short", "Tenant", "Clawful", "The Toothless", "Snatcher", "Nightdeath", "Langarason", "Strawberry",
                    "Zilchless", "Indigo", "Queentooth", "Neverwinter", "Penname", "Gamma", "Alpha", "Jilly", "Bateman",
                    "Nibs", "Glance", "Literally", "The Maneater", "Little", "Stuart", "Renaldo", "Rizzy", "Underdog")
            .build().stream().map(name -> new Name(name)).collect(ImmutableList.toImmutableList());

    public static final record NameGenerationStrategy(NameGenerationStrategy.Kind kind,
            Optional<Examinable.Name> namePin) implements Serializable {
        public enum Kind {
            RANDOM, RANDOM_FALLBACK, PIN_FIRSTNAME, PIN_LASTNAME, CHECK, NO_CHECK;
        }

        public NameGenerationStrategy {
            if (kind == null) {
                throw new IllegalArgumentException("Kind must not be null", new NullPointerException());
            }
            if (namePin == null) {
                throw new IllegalArgumentException("name pin may be empty but must not be null",
                        new NullPointerException());
            }
        }

        public static NameGenerationStrategy ofRandom() {
            return new NameGenerationStrategy(Kind.RANDOM, Optional.empty());
        }

        public static NameGenerationStrategy ofChecked(Examinable.Name name) {
            return new NameGenerationStrategy(Kind.CHECK, Optional.of(name));
        }

        private Examinable.Name randomFirst(DiceSet<Plain> dice) {
            if (dice == null) {
                dice = Plain.dHundred((byte) 1);
            }
            int idex = dice.roll().result() % firstNames.size();
            return firstNames.get(idex);
        }

        private Examinable.Name randomLast(DiceSet<Plain> dice) {
            if (dice == null) {
                dice = Plain.dHundred((byte) 1);
            }
            int idex = dice.roll().result() % lastNames.size();
            return lastNames.get(idex);
        }

        private Examinable.Name randomName(final CreatureBuilderFactory factory, DiceSet<Plain> dice,
                final Examinable.Name first, boolean makeLast, Examinable.Name... middles) {
            if (dice == null) {
                dice = Plain.dHundred((byte) 1);
            }
            StringJoiner sj = new StringJoiner(" ").setEmptyValue("");
            if (middles != null) {
                for (Name name : middles) {
                    if (name == null) {
                        continue;
                    }
                    sj.add(name.toString());
                }
            }
            final String consolidatedMiddles = sj.toString();
            LinkedHashSet<String> created = new LinkedHashSet<>();
            Name madeName = null;
            for (char i = 0; i <= factory.renameAttempts; i++) {
                sj = new StringJoiner(" ");
                if (first == null) {
                    sj.add(this.randomFirst(dice).toString());
                } else {
                    sj.add(first.toString());
                }
                if (consolidatedMiddles != "") {
                    sj.add(consolidatedMiddles);
                }
                if (makeLast) {
                    sj.add(this.randomLast(dice).toString());
                }

                String made = sj.toString();
                created.add(made);
                if (!made.matches(Name.NAME_PATTERN)) {
                    continue;
                }
                madeName = new Name(made);
                if (factory.namesRegister.contains(madeName)) {
                    continue;
                }
                return madeName;
            }

            if (madeName == null) {
                throw new IllegalStateException(String.format(
                        "Pattern '%s' did not allow for combinations, among which '%s'", Name.NAME_PATTERN, created));
            }

            for (int counter = 0; counter < Short.MAX_VALUE; counter++) {
                String made = String.format("%s Count%03d", sj.toString(), counter);
                created.add(made);
                madeName = new Name(made);
                if (!factory.namesRegister.contains(madeName)) {
                    return madeName;
                }
            }

            throw new IllegalStateException(
                    String.format("No created names met '%s' or were not found in the registry", Name.NAME_PATTERN),
                    new IllegalStateException(String.format("Bad names tested", created)));
        }

        public Examinable.Name generateName(final CreatureBuilderFactory factory) throws NameInUseException {
            switch (this.kind) {
            case CHECK:
                if (this.namePin.isPresent()) {
                    if (factory.namesRegister.contains(this.namePin.get())) {
                        throw new NameInUseException(this.namePin.get().toString());
                    }
                    return this.namePin.get();
                }
                throw new IllegalStateException("name generation strategy 'CHECK' should have a name",
                        new NullPointerException("provided name is null"));
            case NO_CHECK:
                if (this.namePin.isPresent()) {
                    return this.namePin.get();
                }
            case PIN_FIRSTNAME:
                if (this.namePin.isPresent()) {
                    return this.randomName(factory, null, this.namePin.orElse(null), true);
                }
            case PIN_LASTNAME:
                if (this.namePin.isPresent()) {
                    return this.randomName(factory, null, null, false, this.namePin.orElse(null));
                }
            case RANDOM_FALLBACK:
                if (this.namePin.isPresent()) {
                    if (!factory.namesRegister.contains(this.namePin.get())) {
                        return this.namePin.get();
                    }
                }
                break;
            case RANDOM:
            default:
                return this.randomName(factory, null, null, true);
            }
            return this.randomName(factory, null, null, true);

        }
    }

    public static class NameInUseException extends Exception {
        final private String name;

        public NameInUseException(String name) {
            super(String.format("Name already in use: '%s'", name));
            this.name = name;
        }

        public NameInUseException(String name, Throwable cause) {
            super(String.format("Name already in use: '%s'", name), cause);
            this.name = name;
        }

        public String name() {
            return this.name;
        }
    }

    @Autowired
    public CreatureBuilderFactory(CreatureRepository repository, EventBus bus) {
        this.repository = repository;
        this.bus = bus;
        this.processorURI = UriComponentsBuilder.fromPath("/builderFactory/creatures")
                .path(UUID.randomUUID().toString()).build().toUri();
        this.bus.register(this);
    }

    @Bean({ "creaturebuilder", "creatureBuilder" })
    @Scope("prototype")
    public Builder builder() {
        final Builder builder = new AutoBuilder_CreatureBuilderFactory_Builder()
                .setScoreModifierBonuses(new EnumMap<>(AttributeScores.class));
        return builder;
    }

    @Override
    public URI processorURI() {
        return this.processorURI;
    }

    @Override
    public Optional<URI> locale() {
        return Optional.of(this.processorURI);
    }

    @AutoBuilder(callMethod = "buildCreature", ofClass = ConcreteCreature.class)
    public abstract static class Builder implements Serializable {
        protected Builder() {
            this.setScoreModifierBonuses(new EnumMap<>(AttributeScores.class));
        }

        protected NameGenerationStrategy nameStrategy;

        public final NameGenerationStrategy getNameStrategy() {
            if (this.nameStrategy == null) {
                throw new IllegalStateException("Property \"strategy\" has not been set");
            }
            return this.nameStrategy;
        }

        public final Builder setNameGenerationStrategy(NameGenerationStrategy strategy) {
            if (strategy == null) {
                throw new NullPointerException("Null strategy");
            }
            this.nameStrategy = strategy;
            return this;
        }

        public final Builder useRandomName() {
            return this.setNameGenerationStrategy(NameGenerationStrategy.ofRandom());
        }

        protected abstract Builder setName(Examinable.Name name);

        protected abstract Examinable.Name getName();

        public Builder setName(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return this.setNameGenerationStrategy(NameGenerationStrategy.ofChecked(eName));
        }

        public abstract Builder setFaction(Faction faction);

        public abstract Builder setVitals(Map<CreatureStats, Integer> vitals);

        protected abstract Map<CreatureStats, Integer> vitals();

        public Builder setHealth(int maxhealth) {
            Map<CreatureStats, Integer> vitals = this.vitals();
            if (vitals == null) {
                vitals = new EnumMap<>(CreatureStats.class);
            }
            vitals.put(CreatureStats.MAX_HEALTH, maxhealth);
            vitals.put(CreatureStats.HEALTH, maxhealth);
            return this.setVitals(vitals);
        }

        public abstract ItemInventory.Builder inventoryBuilder();

        public final Builder addItem(ItemBuilderFactory.LockedItemBuilder... builder) {
            final ItemInventory.Builder set = this.inventoryBuilder();
            set.addContents(builder);
            return this;
        }

        public abstract Builder setScores(Map<AttributeScores, Byte> scores);

        protected abstract Map<AttributeScores, Byte> scores();

        public final Builder scores4d6DropLowest() {
            Map<AttributeScores, Byte> nextScores = new EnumMap<>(AttributeScores.class);
            for (final AttributeScores scoretype : AttributeScores.values()) {
                nextScores.put(scoretype, D6Set.fourD6DropLowestAsByte());
            }
            return this.setScores(nextScores);
        }

        public final Builder setScore(AttributeScores byScore, byte value) {
            Map<AttributeScores, Byte> scoreMap = null;
            try {
                scoreMap = this.scores();
            } catch (IllegalStateException e) {
                scoreMap = new EnumMap<>(AttributeScores.class);
            }
            scoreMap.put(byScore, value);
            return this.setScores(scoreMap);
        }

        public abstract Builder setScoreModifierBonuses(Map<AttributeScores, Byte> bonuses);

        protected abstract Map<AttributeScores, Byte> scoreModifierBonuses();

        public final Builder setScoreModifierBonus(AttributeScores byScore, byte value) {
            Map<AttributeScores, Byte> scoreMap = null;
            try {
                scoreMap = this.scoreModifierBonuses();
            } catch (IllegalStateException e) {
                scoreMap = new EnumMap<>(AttributeScores.class);
            }
            scoreMap.put(byScore, value);
            return this.setScoreModifierBonuses(scoreMap);
        }

        public abstract Builder setEventFunction(@Nullable EventProcessor.EventFunction<Creature> eventProcessor);

        protected abstract ConcreteCreature build();

        public final Creature build(CreatureBuilderFactory factory) {
            if (factory == null) {
                throw new NullPointerException("factory must be provided");
            }
            try {
                this.getName(); // call to see if it is set
            } catch (IllegalStateException e) {
                if (this.nameStrategy == null) {
                    throw new IllegalStateException("Missing required property: nameStrategy");
                }
                try {
                    Name toMake = this.nameStrategy.generateName(factory);
                    this.setName(toMake);
                } catch (NameInUseException e1) {
                    throw new IllegalStateException("cannot use that name", e1);
                }
            }
            ConcreteCreature built = this.build();
            factory.bus.register(built);
            factory.repository.add(built);
            factory.namesRegister.add(this.getName());
            return built;
        }

    }

}
