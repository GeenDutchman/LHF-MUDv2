package com.geendutchman.lhf_mudv2.junction.discord;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import net.dv8tion.jda.api.entities.User;

@Repository
public class DiscordEntityRepository implements Serializable {
    private final BiMap<IEntityID, Long> lookups = Maps.synchronizedBiMap(HashBiMap.<IEntityID, Long>create());
    private final ConcurrentNavigableMap<IEntityID, DiscordEntity> discordEntities = new ConcurrentSkipListMap<>();

    public Optional<Long> otherId(IEntityID id) {
        if (id == null) {
            return Optional.empty();
        }
        synchronized (this.lookups) {
            return Optional.ofNullable(this.lookups.getOrDefault(id, null));
        }
    }

    public Optional<IEntityID> otherId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        synchronized (this.lookups) {
            return Optional.ofNullable(this.lookups.inverse().getOrDefault(id, null));
        }
    }

    public Optional<DiscordEntity> getEntity(IEntityID id) {
        if (id == null) {
            return Optional.empty();
        }
        synchronized (this.lookups) {
            return Optional.ofNullable(this.discordEntities.getOrDefault(id, null));
        }
    }

    public Optional<DiscordEntity> createOrGetEntity(User user) {
        if (user == null) {
            return Optional.empty();
        }
        synchronized (this.lookups) {
            final EntityID entityId = DiscordEntity.idFromUser(user);
            return Optional.ofNullable(this.discordEntities.getOrDefault(entityId, null)).or(() -> {
                final DiscordEntity entity = new DiscordEntity(entityId, new Examinable.Name(user.getId()),
                        user.getId(), Map.of("DiscordTag", user.getAsTag()));
                this.discordEntities.put(entity.identifier(), entity);
                this.lookups.put(entity.identifier(), user.getIdLong());
                return Optional.of(entity);
            });
        }
    }
}
