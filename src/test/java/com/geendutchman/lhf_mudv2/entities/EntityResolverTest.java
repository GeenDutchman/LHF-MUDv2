package com.geendutchman.lhf_mudv2.entities;

import java.net.URI;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.entities.QueryCodec.ItemQueryCodec;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.google.common.truth.Truth;

@SpringBootTest
public class EntityResolverTest {
    @Test
    void testResolve(@Autowired EntityResolver resolver, @Autowired ItemBuilderFactory itemFactory) {
        Item itemOne = ItemBuilderFactory.builder().setName("dingus").build(itemFactory);
        IEntityID id = itemOne.identifier();
        URI uri = id.uri();
        SortedSet<Entity> found = resolver.resolve(uri);
        Truth.assertWithMessage("found the wrong number of entities in '%s", found).that(found).hasSize(1);
        Truth.assertThat(found).contains(itemOne);
        Entity thing = found.first();
        Truth.assertThat(thing.name()).isEqualTo(itemOne.name());
    }

    @Test
    void testResolveItems(@Autowired EntityResolver resolver, @Autowired ItemBuilderFactory itemFactory) {
        Item itemOne = ItemBuilderFactory.builder().setName("dingus").build(itemFactory);
        SortedSet<Entity> found = resolver.resolve(UriComponentsBuilder.fromPath("/items").build().toUri());
        Truth.assertWithMessage("found the wrong number of entities in '%s", found).that(found.size()).isAtLeast(2);
        Truth.assertThat(found).contains(itemOne);
    }

    @Test
    void testResolveItems(@Autowired EntityResolver resolver, @Autowired ItemBuilderFactory itemFactory,
            @Autowired QueryCodec.Factory queryCodecFactory) {
        Item itemOne = ItemBuilderFactory.builder().setName("dingus").build(itemFactory);
        Item itemTwo = ItemBuilderFactory.builder().setName("zoological").setNickname("insect").build(itemFactory);
        ItemQuery itemQuery = ItemQuery.builder().setDisplayName("insect").build();
        ItemQueryCodec codec = queryCodecFactory.defaultItemQueryCodec();
        SortedSet<Entity> found = resolver
                .resolve(codec.toURI(itemQuery, UriComponentsBuilder.fromPath("/items")).build().toUri());
        Truth.assertThat(found).contains(itemTwo);
        Truth.assertThat(found).doesNotContain(itemOne);
        Truth.assertWithMessage("found the wrong number of entities in '%s", found).that(found).hasSize(1);
    }
}
