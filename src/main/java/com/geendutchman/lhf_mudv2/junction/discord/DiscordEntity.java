package com.geendutchman.lhf_mudv2.junction.discord;

import java.util.Map;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.junction.common.DisembodiedEntity;

import net.dv8tion.jda.api.entities.User;

class DiscordEntity extends DisembodiedEntity<String> {

    public static EntityID fromUser(User user) {
        final EntityID id = new EntityID(ENTITY_CLASS_DISEMBODIED, new Name(user.getId()), tsidFactory.create());
        return id;
    }

    protected DiscordEntity(EntityID disembodiedId, Name name, String externalKey, Map<String, String> notes) {
        super(disembodiedId, name, externalKey, notes);
    }
}
