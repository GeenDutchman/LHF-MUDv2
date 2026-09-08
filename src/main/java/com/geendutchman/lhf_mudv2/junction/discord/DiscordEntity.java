package com.geendutchman.lhf_mudv2.junction.discord;

import java.util.Map;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.junction.common.DisembodiedEntity;

import net.dv8tion.jda.api.entities.User;

class DiscordEntity extends DisembodiedEntity<String> {

    public static EntityID idFromUser(User user) {
        final EntityID id = new EntityID(ENTITY_CLASS_DISEMBODIED, new Name(user.getId()), tsidFactory.create());
        return id;
    }

    private final User user;

    protected DiscordEntity(User user) {
        super(idFromUser(user), new Examinable.Name(user.getId()), user.getId(), Map.of("DiscordTag", user.getAsTag()));
        this.user = user;
    }

    public final User getUser() {
        return this.user;
    }

}
