package com.geendutchman.lhf_mudv2.junction.discord;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Service
public class DiscordJunction extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DiscordEntityRepository repo;
    private final MessageBus bus;

    public DiscordJunction(@Autowired DiscordEntityRepository repository, @Autowired MessageBus messageBus)
            throws Exception {
        this.repo = repository;
        this.bus = messageBus;
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (event == null || event.getAuthor().isBot())
            return;
        final Message message = event.getMessage();
        final User author = message.getAuthor();
        final String content = message.getContentRaw();
        if (!content.startsWith("mud") && !content.startsWith("lhf")) {
            // we only care about mud/lhf
            return;
        }

        Optional<IEntityID> entityId = this.repo.otherId(author.getIdLong());
        if (entityId == null || entityId.isEmpty()) {
            this.logger.atInfo().addKeyValue("discordSnowflake", author.getId())
                    .addKeyValue("discordTag", author.getAsTag()).log("Creating new entity for user");
            entityId = this.repo.createOrGetEntity(author).map(e -> e.identifier());
        }

        MessageContext.MessageContextBuilder ctxBuilder = MessageContext.builder().setDestinationId(entityId.get())
                .setSenderId(entityId.get());
        this.repo.getEntity(entityId.get()).ifPresent(dentity -> ctxBuilder.addOther("DiscordEntity", dentity));

        this.bus.send(ctxBuilder.build(), new LHFCommand.LineCommand(LHFCommand.idFactory.create(), content, false));

    }

    public final DiscordEntityRepository repository() {
        return this.repo;
    }

}
