package com.geendutchman.lhf_mudv2.entities.entity;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.events.EventProcessor;

public interface Entity extends Examinable, EventProcessor, Serializable {
    /*
     * A way to specify the item
     */
    public abstract IEntityID identifier();

    @Override
    public default URI processorURI() {
        return this.identifier().uri();
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
            return o1.processorURI().compareTo(o2.processorURI());
        }
    }

    public static <E extends Entity> Comparator<E> getEntityComparator() {
        return new EntityComparator<>();
    }
}
