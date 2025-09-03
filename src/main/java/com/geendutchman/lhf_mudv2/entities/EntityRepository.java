package com.geendutchman.lhf_mudv2.entities;

import org.springframework.lang.NonNull;

import com.geendutchman.lhf_mudv2.entities.EntityContainer.MutableEntityContainer;

/**
 * A marker interface for repositories
 */
public interface EntityRepository<E extends Entity> extends MutableEntityContainer<E> {

    public abstract EntityReference<E> track(@NonNull E entity);

    /**
     * @deprecated prefer {@code track} method
     */
    @Override
    @Deprecated(forRemoval = false, since = "2025-09-02")
    public default boolean add(@NonNull E reference) {
        return this.cargo().add(reference);
    }

}
