package com.geendutchman.lhf_mudv2.dice;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

public record Difficulty<E extends Enum<E>>(ImmutableSortedMap<E, Integer> dcs, Optional<Integer> unflavored)
        implements Predicate<Roll<E>>, Taggable {
    public Difficulty {
        Objects.requireNonNull(dcs, "difficulty classes must not be null");
    }

    @Override
    public boolean test(Roll<E> t) {
        if (t == null) {
            return false;
        }
        return this.test(t, Optional.empty());
    }

    public boolean test(Roll<E> t, Optional<RichOutput.Builder> output) {
        Preconditions.checkNotNull(t, "Roll must not be null");
        Preconditions.checkNotNull(output, "onSuccess must not be null, but may be empty");
        if (dcs.containsKey(t.dice().flavor())) {
            final Integer flavoredCheck = dcs.getOrDefault(t.dice().flavor(), 0);
            if (flavoredCheck > t.roll()) {
                output.ifPresent(onFail -> onFail
                        .addString(String.format("Failed DC %d (%s) with", flavoredCheck, t.dice().flavor().name()))
                        .addTaggable(t));
                return false;
            }

            output.ifPresent(onSuccess -> onSuccess
                    .addString(String.format("Succeeded DC %d (%s) with", flavoredCheck, t.dice().flavor().name()))
                    .addTaggable(t));
            return true;
        } else if (dcs.containsKey(Dice.Plain.UNFLAVORED)) {
            final Integer flavoredCheck = dcs.getOrDefault(Dice.Plain.UNFLAVORED, 0);
            if (flavoredCheck > t.roll()) {
                output.ifPresent(
                        onFail -> onFail.addString(String.format("Failed DC %d with", flavoredCheck)).addTaggable(t));
                return false;
            }
            output.ifPresent(onSuccess -> onSuccess.addString(String.format("Succeeded DC %d with", flavoredCheck))
                    .addTaggable(t));
            return true;
        } else if (this.unflavored.isPresent()) {
            final Integer flavoredCheck = this.unflavored.orElse(0);
            if (flavoredCheck > t.roll()) {
                output.ifPresent(
                        onFail -> onFail.addString(String.format("Failed DC %d with", flavoredCheck)).addTaggable(t));
                return false;
            }
            output.ifPresent(onSuccess -> onSuccess.addString(String.format("Succeeded DC %d with", flavoredCheck))
                    .addTaggable(t));
            return true;
        }
        output.ifPresent(onFail -> onFail.addString("Failed").addTaggable(this));
        return false;
    }

    @Override
    public String tag() {
        return "DIFFICULTY_CLASS";
    }

    @Override
    public String content() {
        StringBuilder builder = new StringBuilder();
        builder.append("DC ");
        for (final Entry<E, Integer> entry : this.dcs.entrySet()) {
            builder.append(entry.getValue());
            if (!Dice.Plain.UNFLAVORED.equals(entry.getKey())) {
                builder.append('(').append(entry.getKey().name()).append(") ");
            } else {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private final static NavigableMap<String, String> BASIC_ATTRIBUTES = Taggable.produceBasicTagAttributes();

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(Difficulty.BASIC_ATTRIBUTES);
    }

}
