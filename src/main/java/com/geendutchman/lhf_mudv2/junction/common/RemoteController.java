package com.geendutchman.lhf_mudv2.junction.common;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.google.common.base.Preconditions;

public abstract class RemoteController<ExternalKey extends Comparable<ExternalKey>, ExternalType>
        implements MessageProcessor {

    public record JunctionId<ExternalKey extends Comparable<ExternalKey>, ExternalType>(EntityID internalId,
            ExternalKey externalId, ExternalType external) implements Comparable<JunctionId<ExternalKey, ?>> {

        public JunctionId {
            Preconditions.checkNotNull(internalId, "EntityId for internal use should not be null");
            Preconditions.checkNotNull(externalId, "external id should not be null");
            Preconditions.checkNotNull(external, "external object should not be null");
        }

        @Override
        public int compareTo(JunctionId<ExternalKey, ?> o) {
            int compare = this.internalId().compareTo(o.internalId());
            if (compare != 0) {
                return compare;
            }
            return this.externalId().compareTo(o.externalId());
        }
    }

    private final MessageProcessorID processorID = new MessageProcessorID(new Examinable.Name("Remote Controller"),
            MessageProcessor.messageProcessorTsidFactory.create());

    final private JunctionId<ExternalKey, ExternalType> id;
    final private ConcurrentNavigableMap<String, String> notes;

    protected RemoteController(JunctionId<ExternalKey, ExternalType> idSet, Map<String, String> someNotes) {
        Preconditions.checkNotNull(idSet, "Set of id's must not be null");
        this.id = idSet;
        this.notes = someNotes != null ? new ConcurrentSkipListMap<>(someNotes) : new ConcurrentSkipListMap<>();
    }

    public RemoteController<ExternalKey, ExternalType> putNote(String key, String value) {
        this.notes.put(key, value);
        return this;
    }

    public MessageProcessorID messageProcessorID() {
        return processorID;
    }

    public ConcurrentNavigableMap<String, String> getNotes() {
        return notes;
    }

    public JunctionId<ExternalKey, ExternalType> getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RemoteController))
            return false;
        RemoteController<?, ?> other = (RemoteController<?, ?>) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Junction [id=").append(id).append(", notes=").append(notes).append("]");
        return builder.toString();
    }

}
