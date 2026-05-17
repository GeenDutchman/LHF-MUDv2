package com.geendutchman.lhf_mudv2.entities.entity;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.google.common.collect.ImmutableSortedMap;

public interface Entity extends Examinable, Serializable {
    /*
     * A way to specify the item
     */
    public abstract IEntityID identifier();

    /**
     * Where does this item find itself?
     * 
     * @return
     */
    public Optional<IEntityID> locale();

    @Override
    public default ImmutableSortedMap<String, String> properties() {
        return ImmutableSortedMap.<String, String>naturalOrder().putAll(Examinable.super.properties())
                .put("identifier", this.identifier().uri().toString()).build();
    }

    public static class EntityComparator<E extends Entity> implements Comparator<E>, Serializable {
        private static Comparator<Examinable> delegate = Examinable.getExaminableComparator();

        @Override
        public int compare(E o1, E o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException("Cannot compare null entities");
            }
            if (o1.equals(o2)) {
                return 0;
            }
            int comparison = delegate.compare(o1, o2);
            if (comparison != 0) {
                return comparison;
            }
            return o1.identifier().compareTo(o2.identifier());
        }
    }

    public static <E extends Entity> Comparator<E> getEntityComparator() {
        return new EntityComparator<>();
    }
}
