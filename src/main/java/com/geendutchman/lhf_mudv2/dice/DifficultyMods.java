package com.geendutchman.lhf_mudv2.dice;

import java.util.Comparator;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * This represents different things one can do to modify a {@link Difficulty}
 */
public sealed interface DifficultyMods<E extends Enum<E>> extends UnaryOperator<Difficulty<E>>, Taggable
        permits DifficultyMods.FlavoredBonus, DifficultyMods.TotalOrFlavored {

    /**
     * A tag for difficulty mods
     * 
     * @see Taggable
     */
    final static Taggable.Tag DIFFICULTY_MODIFIER_TAG = new Taggable.Tag("DIFFICULTY_MODIFIER");

    @Override
    public default Taggable.Tag tag() {
        return DIFFICULTY_MODIFIER_TAG;
    }

    /**
     * This is a small bonus to one particular flavor, like "slashing + 1"
     */
    public record FlavoredBonus<E extends Enum<E>>(E flavor, int bonus) implements DifficultyMods<E> {
        public FlavoredBonus {
            Preconditions.checkNotNull(flavor, "bonus flavor cannot be null");
        }

        @Override
        public Difficulty<E> apply(Difficulty<E> t) {
            return new Difficulty<E>(Stream
                    .concat(Stream.of(Map.entry(this.flavor, this.bonus)), t.dcs().entrySet().stream())
                    .collect(ImmutableSortedMap.toImmutableSortedMap(Comparator.naturalOrder(), entry -> entry.getKey(),
                            entry -> entry.getValue(), (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b))),
                    t.totalOnly());
        }

        @Override
        public String content() {
            return String.format("%s%d to %s", bonus >= 0 ? "+" : "-", bonus, flavor);
        }
    }

    /**
     * Changes whether the difficulty applies to the sum total or best per flavor
     */
    public record TotalOrFlavored<E extends Enum<E>>(boolean totalOnly) implements DifficultyMods<E> {
        @Override
        public Difficulty<E> apply(Difficulty<E> t) {
            return new Difficulty<>(t.dcs(), this.totalOnly);
        }

        @Override
        public String content() {
            if (this.totalOnly) {
                return "Forces comparison between total roll and total dc";
            }
            return "Forces comparison between the best flavor of roll";
        }
    }
}
