package com.geendutchman.lhf_mudv2.entities;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;
import org.springframework.web.util.pattern.PathPatternParser;

import com.geendutchman.lhf_mudv2.entities.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.google.common.collect.ImmutableSortedSet;

public interface EntityResolver {
    SortedSet<Entity> resolve(URI uri);

    public static Pattern entitiesRoot = Pattern.compile("^/entities(?:/|(/.*)?)$");
    public static Pattern roomsRoot = Pattern.compile("^/rooms(?:/|(/.*)?)$");
    public static Pattern itemsRoot = Pattern.compile("^/items(?:/|(/.*)?)$");
    public static Pattern section = Pattern.compile("^/[^/]+(?:/|(/.*)?)$");

    @Component
    public final static class DefaultEntityResolver implements EntityResolver {

        @Autowired
        private final ItemRepository items;

        @Autowired
        private final RoomRepository rooms;

        private final transient SortedMap<PathPattern, BiFunction<PathPattern.PathMatchInfo, URI, SortedSet<Entity>>> routes;

        @Autowired
        public DefaultEntityResolver(ItemRepository items, RoomRepository rooms) {
            this.items = items;
            this.rooms = rooms;
            this.routes = this.createRoutes();
        }

        private SortedMap<PathPattern, BiFunction<PathPattern.PathMatchInfo, URI, SortedSet<Entity>>> createRoutes() {
            SortedMap<PathPattern, BiFunction<PathPattern.PathMatchInfo, URI, SortedSet<Entity>>> routes = new ConcurrentSkipListMap<>(
                    PathPattern.SPECIFICITY_COMPARATOR);
            PathPatternParser pathParser = new PathPatternParser();
            routes.put(pathParser.parse("/entities"), (info, uri) -> {
                ImmutableSortedSet.Builder<Entity> builder = ImmutableSortedSet.orderedBy(Entity.getEntityComparator());
                this.items.items().forEach(item -> builder.add(item));
                this.rooms.rooms().forEach(room -> builder.add(room));
                return builder.build();
            });
            routes.put(pathParser.parse("/rooms"), (info, uri) -> {
                ImmutableSortedSet.Builder<Entity> builder = ImmutableSortedSet.orderedBy(Entity.getEntityComparator());
                this.rooms.rooms().forEach(room -> builder.add(room));
                return builder.build();
            });
            routes.put(pathParser.parse("/items"), (info, uri) -> {
                ImmutableSortedSet.Builder<Entity> builder = ImmutableSortedSet.orderedBy(Entity.getEntityComparator());
                this.items.items().forEach(item -> builder.add(item));
                return builder.build();
            });
            routes.put(pathParser.parse("/rooms/{room-name}/{room-id}"), (info, uri) -> {
                UUID id;
                try {
                    id = UUID.fromString(info.getUriVariables().getOrDefault("room-id", null));
                } catch (IllegalArgumentException | NullPointerException e) {
                    return ImmutableSortedSet.of();
                }
                EntityID entityID = new EntityID("rooms", info.getUriVariables().getOrDefault("room-name", ""), id);
                RoomID roomID = new RoomID(entityID);
                Optional<Room> found = this.rooms.byRoomID(roomID);
                if (found.isPresent()) {
                    return ImmutableSortedSet.orderedBy(Entity.getEntityComparator()).add(found.get()).build();
                }
                return ImmutableSortedSet.of();
            });
            routes.put(pathParser.parse("/rooms/{room-name}/{room-id}/items"), (info, url) -> {
                UUID id;
                try {
                    id = UUID.fromString(info.getUriVariables().getOrDefault("room-id", null));
                } catch (IllegalArgumentException | NullPointerException e) {
                    return ImmutableSortedSet.of();
                }
                EntityID entityID = new EntityID("rooms", info.getUriVariables().getOrDefault("room-name", ""), id);
                RoomID roomID = new RoomID(entityID);
                Optional<Room> found = this.rooms.byRoomID(roomID);
                if (found.isPresent()) {
                    return ImmutableSortedSet.orderedBy(Entity.getEntityComparator())
                            .addAll(found.get().inventory().items().toList()).build();
                }
                return ImmutableSortedSet.of();
            });
            routes.put(pathParser.parse("/rooms/{room-name}/{room-id}/items/{item-name}/{item-id}"), (info, url) -> {
                UUID roomUUID;
                try {
                    roomUUID = UUID.fromString(info.getUriVariables().getOrDefault("room-id", null));
                } catch (IllegalArgumentException | NullPointerException e) {
                    return ImmutableSortedSet.of();
                }
                RoomID roomID = new RoomID(
                        new EntityID("rooms", info.getUriVariables().getOrDefault("room-name", ""), roomUUID));
                Optional<Room> found = this.rooms.byRoomID(roomID);
                if (found.isPresent()) {
                    UUID itemUUID;
                    try {
                        itemUUID = UUID.fromString(info.getUriVariables().getOrDefault("item-id", null));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        return ImmutableSortedSet.of();
                    }
                    ItemID itemID = new ItemID(
                            new EntityID("items", info.getUriVariables().getOrDefault("item-name", ""), itemUUID));
                    Optional<Item> retrieved = found.get().inventory().byItemID(itemID);
                    if (retrieved.isPresent()) {
                        return ImmutableSortedSet.orderedBy(Entity.getEntityComparator()).add(retrieved.get()).build();
                    }
                }
                return ImmutableSortedSet.of();
            });
            return routes;
        }

        @Override
        public SortedSet<Entity> resolve(final URI uri) {
            final String path = uri.getPath();

            for (Entry<PathPattern, BiFunction<PathMatchInfo, URI, SortedSet<Entity>>> route : this.routes.entrySet()) {
                PathMatchInfo matchInfo = route.getKey().matchAndExtract(PathContainer.parsePath(path));
                if (matchInfo != null) {
                    return route.getValue().apply(matchInfo, uri);
                }
            }
            return ImmutableSortedSet.of();
        }

    }
}
