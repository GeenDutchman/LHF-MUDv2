package com.geendutchman.lhf_mudv2.dice;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * This represents the difficulty of some task, flavored according to the
 * parameterized type
 */
public record Difficulty<E extends Enum<E>>(ImmutableSortedMap<E, Integer> dcs, boolean totalOnly)
        implements Predicate<RollSet<E>>, Taggable {

    /**
     * @see Taggable#produceBasicTagProperties
     */
    public final static NavigableMap<String, String> BASIC_PROPERTIES = Taggable.produceBasicTagProperties();

    @Override
    public ImmutableSortedMap<String, String> properties() {
        return ImmutableSortedMap.copyOf(Difficulty.BASIC_PROPERTIES);
    }

    /**
     * A tag for the difficulty
     * 
     * @see Taggable
     */
    final static Taggable.Tag DIFFICULTY_CLASS_TAG = new Taggable.Tag("DIFFICULTY_CLASS");

    @Override
    public Taggable.Tag tag() {
        return DIFFICULTY_CLASS_TAG;
    }

    /**
     * Constructor for the difficulty
     * 
     * @param dcs       difficulty classes
     * @param totalOnly if only the sum total of the roll matters, or if the best
     *                  according to flavor is accounted for
     */
    public Difficulty {
        Objects.requireNonNull(dcs, "difficulty classes must not be null");
        Preconditions.checkArgument(dcs.size() > 0, "difficulty classes must not be empty");
    }

    @Override
    public boolean test(RollSet<E> t) {
        if (t == null) {
            return false;
        }
        return this.test(t, Optional.empty());
    }

    /**
     * Sum of all the values of the difficulty classes
     * 
     * @return
     */
    public int sum() {
        return this.dcs.values().stream().filter(i -> i != null).mapToInt(i -> i).sum();
    }

    /**
     * Test a {@link RollSet} and optionally build a message about it
     * 
     * @param t      {@link RollSet} to test
     * @param output {@link Optional} {@link RichOutput.Builder} to build a message
     *               about the test results
     * @return true if the rollset passes this Difficulty
     */
    public boolean test(RollSet<E> t, Optional<RichOutput.Builder> output) {
        Preconditions.checkNotNull(t, "Roll must not be null");
        Preconditions.checkNotNull(output, "output must not be null, but may be empty");

        if (this.totalOnly) {
            final Integer sum = this.sum();
            if (t.result() >= sum) {
                output.ifPresent(builder -> builder.addString("Succeeded").addPolymorphic(this).addString("with")
                        .addPolymorphic(t));
                return true;
            }
            output.ifPresent(
                    builder -> builder.addString("Failed").addPolymorphic(this).addString("with").addPolymorphic(t));
            return false;
        }

        int maxExcess = Integer.MIN_VALUE;
        int closestDistance = Integer.MAX_VALUE;
        AtomicReference<E> maxEr = new AtomicReference<>();
        AtomicReference<E> closestEr = new AtomicReference<>();

        for (final Entry<E, Integer> dc : this.dcs.entrySet()) {
            final Integer flavorRoll = t.rolls().getOrDefault(dc.getKey(), null);
            final Integer flavorDC = dc.getValue();

            if (flavorRoll == null || flavorDC == null) {
                continue;
            }

            int excess = flavorRoll - flavorDC;

            if (excess > maxExcess) {
                maxExcess = excess;
                maxEr.set(dc.getKey());
            }

            if (excess < 0) {
                int dist = -1 * excess;
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestEr.set(dc.getKey());
                }
            }
        }

        if (maxEr.get() != null && maxExcess > 0) {
            output.ifPresent(builder -> builder.addString("Succeeded").addPolymorphic(this).addString("with")
                    .addPolymorphic(t.rowContent(maxEr.get())));
            return true;
        } else if (closestEr.get() != null) {
            output.ifPresent(builder -> builder.addString("Failed").addPolymorphic(this).addString("with")
                    .addPolymorphic(t.rowContent(closestEr.get())));
            return false;
        }
        output.ifPresent(builder -> builder.addString("Failed").addPolymorphic(this).addString("because no element of")
                .addPolymorphic(t).addString("matched"));
        return false;
    }

    @Override
    public String content() {
        StringBuilder builder = new StringBuilder();
        builder.append("DC ");
        if (this.totalOnly) {
            builder.append(this.sum());
            return builder.toString().trim();
        }
        for (final Entry<E, Integer> entry : this.dcs.entrySet()) {
            builder.append(entry.getValue());
            if (!Plain.UNFLAVORED.equals(entry.getKey())) {
                builder.append('(').append(entry.getKey().name()).append(") ");
            } else {
                builder.append(' ');
            }
        }
        return builder.toString().trim();
    }

}
