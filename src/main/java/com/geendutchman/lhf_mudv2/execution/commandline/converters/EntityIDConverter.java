package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.github.f4b6a3.tsid.Tsid;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

@Component
public record EntityIDConverter() implements ITypeConverter<IEntityID> {

    @Override
    public IEntityID convert(String value) throws Exception {
        return this.convertEntityID(value);
    }

    public EntityID convertEntityID(String value) throws Exception {
        try {
            URI asUri = new URI(value);
            String path = asUri.getPath();
            String[] splits = path.split("/");
            if (splits.length != 3) {
                throw new TypeConversionException(
                        String.format("An entity id only has 3 parts not %d: %v", splits.length, splits));
            }
            Taggable.Tag entityClass = new Taggable.Tag(splits[0]);
            Examinable.Name name = new Examinable.Name(splits[1]);
            Tsid tsid = Tsid.from(splits[2]);

            return new EntityID(entityClass, name, tsid);
        } catch (URISyntaxException ue) {
            throw new TypeConversionException("this does not fit a URI");
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new TypeConversionException(String.format("Does not match the format of an entity id -> %s", e));
        }
    }

    public ItemID convertItemID(String value) throws Exception {
        EntityID asEntity = this.convertEntityID(value);
        return new ItemID(asEntity);
    }

    public CreatureID convertCreatureID(String value) throws Exception {
        EntityID asEntity = this.convertEntityID(value);
        return new CreatureID(asEntity);
    }

    public RoomID convertRoomID(String value) throws Exception {
        EntityID asEntity = this.convertEntityID(value);
        return new RoomID(asEntity);
    }

}
