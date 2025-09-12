package com.geendutchman.lhf_mudv2.dice;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringJoiner;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

record RollSet<E extends Enum<E>>(DiceSet<E> diceSet, ImmutableSortedMap<E, Integer> rolls,
        ImmutableMap<E, String> notes, Optional<RollSet<E>> origin) implements Taggable {

    RollSet {
        Preconditions.checkNotNull(diceSet, "Diceset should not be null");
        Preconditions.checkArgument(!diceSet.isEmpty(), "DiceSet should not be empty");
        Preconditions.checkNotNull(rolls, "rolls may be empty but may not be null");
        Preconditions.checkNotNull(notes, "optional note must be empty, not null");
        Preconditions.checkNotNull(origin, "optional origin must be empty, not null");
    }

    public DiceSet<E> rollable() {
        return this.diceSet;
    }

    public ImmutableSet<E> flavors() {
        return this.rolls.keySet();
    }

    public int result() {
        return this.rolls.values().stream().filter(one -> one != null).mapToInt(roll -> roll).sum();
    }

    final static Taggable.Tag ROLL_SET_TAG = new Taggable.Tag("ROLL_RESULT_SET");

    @Override
    public Taggable.Tag tag() {
        return ROLL_SET_TAG;
    }

    public String rowContent(final E row) {
        StringBuilder sb = new StringBuilder();
        if (this.origin.isPresent()) {
            sb.append(this.origin.get().rowContent(row));
            if (this.notes.containsKey(row)) {
                sb.append(' ').append(this.notes.getOrDefault(row, ""));
            }
        } else {
            sb.append(this.diceSet.rowContent(row));
        }
        sb.append("-->").append(this.rolls.getOrDefault(row, 0));
        return sb.toString();
    }

    @Override
    public String content() {
        StringJoiner sj = new StringJoiner("+").setEmptyValue("");
        int sum = 0;
        for (final Entry<E, Integer> rolline : this.rolls.entrySet()) {
            sum += rolline.getValue();
            sj.add(this.rowContent(rolline.getKey()));
        }
        return String.format("%s=%d", sj.toString(), sum);

    }
}
