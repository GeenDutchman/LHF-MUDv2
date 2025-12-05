package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputSubject;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.google.common.truth.Truth;

@SpringBootTest
public class RoomTest {

    @Test
    void testDisplay(@Autowired RoomBuilderFactory roomFactory, @Autowired ItemBuilderFactory itemFactory) {
        RoomBuilderFactory.BuildRoom roomBuilder = RoomBuilderFactory.builder().setName("First room")
                .setRoomDescription(Optional.of(RichOutput.builder().addString("This is a room").build()));
        roomBuilder.addItem(ItemBuilderFactory.builder().setName("dingus").lock());
        roomBuilder.addItem(ItemBuilderFactory.builder().setName("dongus").setNickname("alakazam").lock());
        final Room built = roomBuilder.build(roomFactory);
        RoomSubject.assertThat(built).name().isEqualTo("First room");
        final Optional<RichOutput> desc = built.description();
        Truth.assertThat(desc).isPresent();
        RichOutputSubject.assertThat(desc.get()).asXMLString().ignoringCase().doesNotContain("bug");
        RichOutputSubject.assertThat(desc.get()).asXMLString().contains("This");
    }

    @Test
    void testDelta(@Autowired RoomBuilderFactory roomFactory, @Autowired ItemBuilderFactory itemFactory) {
        RoomBuilderFactory.BuildRoom roomBuilder = RoomBuilderFactory.builder().setName("First room")
                .setRoomDescription(Optional.of(RichOutput.builder().addString("This is a room").build()));
        roomBuilder.addItem(ItemBuilderFactory.builder().setName("dingus").lock());
        roomBuilder.addItem(ItemBuilderFactory.builder().setName("dongus").setNickname("alakazam").lock());
        final Room built = roomBuilder.build(roomFactory);
        RoomSubject.assertThat(built).name().isEqualTo("First room");

        RoomSubject.assertThat(built).items().queryAll(ItemQuery.builder().setDisplayName("lullaby").build()).isEmpty();
        Room.Delta delta = Room.Delta
                .ofItemToAdd(ItemBuilderFactory.builder().setName("lullaby").lock().build(itemFactory));
        built.applyDelta(delta);
        RoomSubject.assertThat(built).items().queryAll(ItemQuery.builder().setDisplayName("lullaby").build())
                .isNotEmpty();
    }
}
