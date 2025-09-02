package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputSubject;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room.BuildRoom;
import com.google.common.truth.Truth;

@SpringBootTest
public class RoomTest {

    @Test
    void testDisplay() {
        BuildRoom roomBuilder = Room.builder().setName("First room");
        roomBuilder.addItem(Item.builder().setName("dingus").lock());
        roomBuilder.addItem(Item.builder().setName("dongus").setNickname("alakazam").lock());
        final Room built = roomBuilder.build();
        RoomSubject.assertThat(built).name().isEqualTo("First room");
        final Optional<RichOutput> desc = built.description();
        Truth.assertThat(desc).isPresent();
        RichOutputSubject.assertThat(desc.get()).asXMLString().contains("zipper");
    }
}
