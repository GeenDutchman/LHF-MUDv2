package com.geendutchman.lhf_mudv2.entities;

import com.geendutchman.lhf_mudv2.entities.EntityContainer.MutableEntityContainer;

/**
 * A marker interface for repositories
 */
public interface EntityRepository<E extends Entity> extends MutableEntityContainer<E> {

}
