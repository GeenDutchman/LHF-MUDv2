package com.geendutchman.lhf_mudv2.dice;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;

public sealed interface RollMods<E extends Enum<E>> extends UnaryOperator<RollSet<E>>, Taggable
        permits RollMods.Zeroed, RollMods.Doubled, RollMods.Halved, RollMods.Added {

    final static Taggable.Tag ROLL_MODIFIER_TAG = new Taggable.Tag("ROLL_MODIFIER");

    @Override
    public default Taggable.Tag tag() {
        return ROLL_MODIFIER_TAG;
    }

    private static <E extends Enum<E>> ImmutableMap<E, String> appendNote(ImmutableMap<E, String> notes, E flavor,
            String s) {
        final ImmutableMap<E, String> justNote = ImmutableMap.of(flavor, s);
        if (notes == null) {
            return justNote;
        }
        return Streams.concat(justNote.entrySet().stream(), notes.entrySet().stream()).filter(entry -> entry != null)
                .collect(ImmutableMap.toImmutableMap(entry -> entry.getKey(), entry -> entry.getValue(),
                        (left, right) -> {
                            if (left == null && right == null) {
                                return null;
                            } else if (left != null && right == null) {
                                return left;
                            } else if (left == null && right != null) {
                                return right;
                            }
                            return left + right;
                        }));
    }

    public record Zeroed<E extends Enum<E>>(E flavor) implements RollMods<E> {
        public Zeroed {
            Preconditions.checkNotNull(flavor, "flavor to zero cannot be null");
        }

        @Override
        public RollSet<E> apply(RollSet<E> t) {
            return new RollSet<>(t.diceSet(),
                    t.rolls().entrySet().stream().filter(entry -> entry != null && !flavor.equals(entry.getKey()))
                            .collect(ImmutableSortedMap.toImmutableSortedMap(Comparator.<E>naturalOrder(),
                                    entry -> entry.getKey(), entry -> entry.getValue())),
                    RollMods.appendNote(t.notes(), flavor, "(zeroed)"), Optional.of(t));
        }

        @Override
        public String content() {
            return String.format("zeroes out %s", flavor);
        }
    }

    public record Doubled<E extends Enum<E>>(E flavor) implements RollMods<E> {
        public Doubled {
            Preconditions.checkNotNull(flavor, "flavor to double cannot be null");
        }

        @Override
        public RollSet<E> apply(RollSet<E> t) {
            return new RollSet<>(t.diceSet(),
                    t.rolls().entrySet().stream().filter(entry -> entry != null).collect(ImmutableSortedMap
                            .toImmutableSortedMap(Comparator.<E>naturalOrder(), entry -> entry.getKey(), entry -> {
                                if (flavor.equals(entry.getKey())) {
                                    return entry.getValue() * 2;
                                }
                                return entry.getValue();
                            })),
                    RollMods.appendNote(t.notes(), flavor, "(doubled)"), Optional.of(t));
        }

        @Override
        public String content() {
            return String.format("doubles %s", flavor);
        }
    }

    public record Halved<E extends Enum<E>>(E flavor) implements RollMods<E> {
        public Halved {
            Preconditions.checkNotNull(flavor, "flavor to halve cannot be null");
        }

        @Override
        public RollSet<E> apply(RollSet<E> t) {
            return new RollSet<>(t.diceSet(),
                    t.rolls().entrySet().stream().filter(entry -> entry != null).collect(ImmutableSortedMap
                            .toImmutableSortedMap(Comparator.<E>naturalOrder(), entry -> entry.getKey(), entry -> {
                                if (flavor.equals(entry.getKey())) {
                                    return entry.getValue() / 2;
                                }
                                return entry.getValue();
                            })),
                    RollMods.appendNote(t.notes(), flavor, "(halved)"), Optional.of(t));
        }

        @Override
        public String content() {
            return String.format("halves %s", flavor);
        }
    }

    public record Added<E extends Enum<E>>(E flavor, int bonus) implements RollMods<E> {
        public Added {
            Preconditions.checkNotNull(flavor, "flavor to add bonus cannot be null");
            Preconditions.checkArgument(bonus != 0, "bonus to add must not be 0");
        }

        @Override
        public RollSet<E> apply(RollSet<E> t) {
            return new RollSet<>(t.diceSet(),
                    Streams.concat(ImmutableMap.of(flavor, bonus).entrySet().stream(), t.rolls().entrySet().stream())
                            .filter(entry -> entry != null && entry.getValue() != null)
                            .collect(ImmutableSortedMap.toImmutableSortedMap(Comparator.<E>naturalOrder(),
                                    entry -> entry.getKey(), entry -> entry.getValue(),
                                    (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b))),
                    RollMods.appendNote(t.notes(), flavor,
                            String.format("(%s %d)", bonus >= 0 ? "added" : "subtracted", bonus)),
                    Optional.of(t));
        }

        @Override
        public String content() {
            return String.format("%s%d to %s", bonus >= 0 ? "+" : "-", bonus, flavor);
        }
    }
}
