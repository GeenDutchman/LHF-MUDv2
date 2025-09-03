package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputSubject;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.Room.BuildRoom;
import com.google.common.truth.Truth;

@SpringBootTest
public class RoomTest {

    @Test
    void testDisplay(@Autowired RoomBuilderFactory roomFactory, @Autowired ItemBuilderFactory itemFactory) {
        BuildRoom roomBuilder = roomFactory.builder().setName("First room")
                .setRoomDescription(Optional.of(RichOutput.builder().addString("This is a room").build()));
        roomBuilder.addItem(itemFactory.builder().setName("dingus").lock());
        roomBuilder.addItem(itemFactory.builder().setName("dongus").setNickname("alakazam").lock());
        final Room built = roomBuilder.build();
        RoomSubject.assertThat(built).name().isEqualTo("First room");
        final Optional<RichOutput> desc = built.description();
        Truth.assertThat(desc).isPresent();
        RichOutputSubject.assertThat(desc.get()).asXMLString().ignoringCase().doesNotContain("bug");
        RichOutputSubject.assertThat(desc.get()).asXMLString().contains("alakazam");
    }

    @Test
    void testDelta(@Autowired RoomBuilderFactory roomFactory, @Autowired ItemBuilderFactory itemFactory) {
        BuildRoom roomBuilder = roomFactory.builder().setName("First room")
                .setRoomDescription(Optional.of(RichOutput.builder().addString("This is a room").build()));
        roomBuilder.addItem(itemFactory.builder().setName("dingus").lock());
        roomBuilder.addItem(itemFactory.builder().setName("dongus").setNickname("alakazam").lock());
        final Room built = roomBuilder.build();
        RoomSubject.assertThat(built).name().isEqualTo("First room");

        RichOutputSubject.assertThat(built.description().get()).asXMLString().doesNotContain("lullaby");
        Room.Delta delta = Room.Delta.ofItemBuilder(itemFactory.builder().setName("lullaby").lock());
        built.applyDelta(delta);
        RichOutputSubject.assertThat(built.description().get()).asXMLString().contains("lullaby");
    }
}
