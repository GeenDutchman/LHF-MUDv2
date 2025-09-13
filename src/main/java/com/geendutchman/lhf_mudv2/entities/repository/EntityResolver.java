package com.geendutchman.lhf_mudv2.entities.repository;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPattern.PathRemainingMatchInfo;
import org.springframework.web.util.pattern.PathPatternParser;

import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomContainer;
import com.geendutchman.lhf_mudv2.entities.room.RoomQuery;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.google.common.collect.ImmutableSortedSet;

public interface EntityResolver {
    SortedSet<Entity> resolve(URI uri);

    @Service
    public final static class DefaultEntityResolver implements EntityResolver {

        @Autowired
        private final ItemRepository items;

        @Autowired
        private final RoomRepository rooms;

        @Autowired
        private final QueryCodec.Factory queryCodecFactory;

        private final transient SortedMap<PathPattern, BiFunction<PathPattern.PathRemainingMatchInfo, URI, SortedSet<Entity>>> routes;

        @Autowired
        public DefaultEntityResolver(QueryCodec.Factory queryCodecFactory, ItemRepository items, RoomRepository rooms) {
            this.queryCodecFactory = queryCodecFactory;
            this.items = items;
            this.rooms = rooms;
            this.routes = this.createRoutes();
        }

        private Optional<Item> getItem(PathRemainingMatchInfo info, URI uri, ItemContainer itemContainer) {
            if (itemContainer == null || uri == null || info == null) {
                return Optional.empty();
            }
            UUID itemUUID;
            try {
                itemUUID = UUID.fromString(info.getUriVariables().getOrDefault("item-id", null));
            } catch (IllegalArgumentException | NullPointerException e) {
                return Optional.empty();
            }
            ItemID itemID = new ItemID(
                    new EntityID("items", info.getUriVariables().getOrDefault("item-name", ""), itemUUID));
            Optional<Item> retrieved = itemContainer.byItemID(itemID);
            return retrieved;
        }

        private SortedSet<Entity> resolveItem(PathRemainingMatchInfo info, URI uri, ItemContainer itemContainer) {
            Optional<Item> retrieved = this.getItem(info, uri, itemContainer);
            if (retrieved != null && retrieved.isPresent()) {
                return ImmutableSortedSet.orderedBy(Entity.getEntityComparator()).add(retrieved.get()).build();
            }
            return ImmutableSortedSet.of();
        }

        private SortedSet<Entity> resolveAllEntities(PathRemainingMatchInfo info, URI uri, ItemContainer itemContainer,
                RoomContainer rooms) {
            ImmutableSortedSet.Builder<Entity> builder = ImmutableSortedSet.orderedBy(Entity.getEntityComparator());
            EntityQuery query = queryCodecFactory.defaultEntityQueryCodec().fromURI(uri);
            this.items.items().filter(query).forEach(item -> builder.add(item));
            this.rooms.rooms().filter(query).forEach(room -> builder.add(room));
            return builder.build();
        }

        private SortedSet<Entity> resolveItems(PathRemainingMatchInfo info, URI uri, ItemContainer itemContainer) {
            ImmutableSortedSet.Builder<Entity> builder = ImmutableSortedSet.orderedBy(Entity.getEntityComparator());
            ItemQuery query = queryCodecFactory.defaultItemQueryCodec().fromURI(uri);
            this.items.items().filter(query).forEach(item -> builder.add(item));
            return builder.build();
        }

        private SortedSet<Entity> resolveRooms(PathRemainingMatchInfo info, URI uri, RoomContainer roomContainer) {
            ImmutableSortedSet.Builder<Entity> builder = ImmutableSortedSet.orderedBy(Entity.getEntityComparator());
            RoomQuery query = queryCodecFactory.defaultRoomQueryCodec().fromURI(uri);
            this.rooms.rooms().filter(query).forEach(room -> builder.add(room));
            return builder.build();
        }

        private Optional<Room> getRoom(PathRemainingMatchInfo info, URI uri, RoomContainer roomContainer) {
            UUID id;
            try {
                id = UUID.fromString(info.getUriVariables().getOrDefault("room-id", null));
            } catch (IllegalArgumentException | NullPointerException e) {
                return Optional.empty();
            }
            EntityID entityID = new EntityID("rooms", info.getUriVariables().getOrDefault("room-name", ""), id);
            RoomID roomID = new RoomID(entityID);
            Optional<Room> found = this.rooms.byRoomID(roomID);
            return found;
        }

        private SortedSet<Entity> resolveRoom(PathRemainingMatchInfo info, URI uri, RoomContainer roomContainer) {
            Optional<Room> found = this.getRoom(info, uri, roomContainer);
            if (found != null && found.isPresent()) {
                return ImmutableSortedSet.orderedBy(Entity.getEntityComparator()).add(found.get()).build();
            }
            return ImmutableSortedSet.of();
        }

        private SortedSet<Entity> resolveRoomItems(PathRemainingMatchInfo info, URI uri, RoomContainer roomContainer) {
            Optional<Room> found = this.getRoom(info, uri, roomContainer);
            if (found != null && found.isPresent()) {
                return this.resolveItems(info, uri, found.get());
            }
            return ImmutableSortedSet.of();
        }

        private SortedSet<Entity> resolveItemInRoom(PathRemainingMatchInfo info, URI uri, RoomContainer roomContainer) {
            Optional<Room> found = this.getRoom(info, uri, roomContainer);
            if (found != null && found.isPresent()) {
                return this.resolveItem(info, uri, found.get());
            }
            return ImmutableSortedSet.of();
        }

        private SortedMap<PathPattern, BiFunction<PathPattern.PathRemainingMatchInfo, URI, SortedSet<Entity>>> createRoutes() {
            SortedMap<PathPattern, BiFunction<PathPattern.PathRemainingMatchInfo, URI, SortedSet<Entity>>> routes = new ConcurrentSkipListMap<>();
            PathPatternParser pathParser = new PathPatternParser();
            routes.put(pathParser.parse("/entities"), (info, uri) -> {
                return this.resolveAllEntities(info, uri, items, rooms);
            });
            routes.put(pathParser.parse("/items"), (info, uri) -> {
                return this.resolveItems(info, uri, items);
            });
            routes.put(pathParser.parse("/items/{item-name}/{item-id:[a-fA-F0-9-]+}"), (info, url) -> {
                return this.resolveItem(info, url, items);
            });
            routes.put(pathParser.parse("/rooms"), (info, uri) -> {
                return this.resolveRooms(info, uri, rooms);
            });
            routes.put(pathParser.parse("/rooms/{room-name}/{room-id:[a-fA-F0-9-]+}"), (info, uri) -> {
                return this.resolveRoom(info, uri, rooms);
            });
            routes.put(pathParser.parse("/rooms/{room-name}/{room-id:[a-fA-F0-9-]+}/items"), (info, url) -> {
                return this.resolveRoomItems(info, url, rooms);
            });
            routes.put(
                    pathParser.parse(
                            "/rooms/{room-name}/{room-id:[a-fA-F0-9-]+}/items/{item-name}/{item-id:[a-fA-F0-9-]+}"),
                    (info, url) -> {
                        return this.resolveItemInRoom(info, url, rooms);
                    });
            return routes;
        }

        @Override
        public SortedSet<Entity> resolve(final URI uri) {
            final String path = uri.getPath();

            for (Entry<PathPattern, BiFunction<PathRemainingMatchInfo, URI, SortedSet<Entity>>> route : this.routes
                    .reversed().entrySet()) {
                PathPattern routePath = route.getKey();
                PathRemainingMatchInfo matchInfo = routePath.matchStartOfPath(PathContainer.parsePath(path));
                if (matchInfo != null) {
                    return route.getValue().apply(matchInfo, uri);
                }
            }
            return ImmutableSortedSet.of();
        }

    }
}
