package com.geendutchman.lhf_mudv2.entities.repository;

import java.net.URI;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery.EntityQueryBuilder;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.room.RoomQuery;

public interface QueryCodec {

    public final class EntityQueryCodec implements QueryCodec {
        private EntityQueryCodec() {
        }

        @Override
        public EntityQuery fromURI(UriComponents uri) {
            EntityQueryBuilder builder = EntityQuery.builder();
            MultiValueMap<String, String> uriQuery = uri.getQueryParams();
            builder.fromKeyValue(uriQuery.asSingleValueMap());
            return builder.build();
        }

        @Override
        public EntityQuery fromURI(URI uri) {
            return this.fromURI(UriComponentsBuilder.fromUri(uri).build());
        }

    }

    public static final EntityQueryCodec ENTITY_QUERY_CODEC = new EntityQueryCodec();

    public final class ItemQueryCodec implements QueryCodec {
        private ItemQueryCodec() {
        }

        @Override
        public ItemQuery fromURI(UriComponents uri) {
            ItemQuery.Builder builder = ItemQuery.builder();
            MultiValueMap<String, String> uriQuery = uri.getQueryParams();
            builder.fromKeyValue(uriQuery.asSingleValueMap());
            return builder.build();
        }

        @Override
        public ItemQuery fromURI(URI uri) {
            return this.fromURI(UriComponentsBuilder.fromUri(uri).build());
        }

    }

    public static final ItemQueryCodec ITEM_QUERY_CODEC = new ItemQueryCodec();

    public final class RoomQueryCodec implements QueryCodec {
        private RoomQueryCodec() {
        }

        @Override
        public RoomQuery fromURI(UriComponents uri) {
            RoomQuery.Builder builder = RoomQuery.builder();
            MultiValueMap<String, String> uriQuery = uri.getQueryParams();
            builder.fromKeyValue(uriQuery.asSingleValueMap());
            return builder.build();
        }

        @Override
        public RoomQuery fromURI(URI uri) {
            return this.fromURI(UriComponentsBuilder.fromUri(uri).build());
        }
    }

    @Service
    public final class Factory {
        private final Map<String, QueryCodec> codecs;

        public Factory() {
            this.codecs = Map.of("item", ITEM_QUERY_CODEC, "room", ROOM_QUERY_CODEC, "entity", ENTITY_QUERY_CODEC);
        }

        public Factory(@NonNull Map<String, QueryCodec> codecs) {
            this.codecs = Map.copyOf(codecs);
        }

        public QueryCodec get(String key) {
            return this.codecs.getOrDefault(key, ENTITY_QUERY_CODEC);
        }

        public ItemQueryCodec defaultItemQueryCodec() {
            return ITEM_QUERY_CODEC;
        }

        public RoomQueryCodec defaultRoomQueryCodec() {
            return ROOM_QUERY_CODEC;
        }

        public EntityQueryCodec defaultEntityQueryCodec() {
            return ENTITY_QUERY_CODEC;
        }

    }

    public static final RoomQueryCodec ROOM_QUERY_CODEC = new RoomQueryCodec();

    public static IEntityQuery<? extends Entity> defaultFromURI(UriComponents uri) {
        return ENTITY_QUERY_CODEC.fromURI(uri);
    }

    public static IEntityQuery<? extends Entity> defaultFromURI(URI uri) {
        return ENTITY_QUERY_CODEC.fromURI(uri);
    }

    public static UriComponentsBuilder defaultToURI(IEntityQuery<?> query, UriComponentsBuilder builder) {
        return ENTITY_QUERY_CODEC.toURI(query, builder);
    }

    public static UriComponentsBuilder defaultToURI(IEntityQuery<?> query) {
        return ENTITY_QUERY_CODEC.toURI(query);
    }

    public default UriComponentsBuilder toURI(IEntityQuery<?> query, UriComponentsBuilder builder) {
        Map<String, String> kv = query.toKeyValue();
        for (Map.Entry<String, String> entry : kv.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public default UriComponentsBuilder toURI(IEntityQuery<?> query) {
        return this.toURI(query, UriComponentsBuilder.newInstance());
    }

    public abstract IEntityQuery<? extends Entity> fromURI(UriComponents uri);

    public default IEntityQuery<? extends Entity> fromURI(URI uri) {
        return this.fromURI(UriComponentsBuilder.fromUri(uri).build());
    }

}
