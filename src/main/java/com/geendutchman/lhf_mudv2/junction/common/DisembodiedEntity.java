package com.geendutchman.lhf_mudv2.junction.common;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;

/**
 * The DisembodiedEntity is deliberately not in the 'entity' packages or related
 * because it represents a temporary, proto-creature. It is not meant to be
 * stored much of anywhere persistent.
 */
public class DisembodiedEntity<ExternalKey extends Comparable<ExternalKey>> implements Entity {
    public static final Taggable.Tag ENTITY_CLASS_DISEMBODIED = new Tag("disembodied");
    protected static final TsidFactory tsidFactory = TsidFactory
            .newInstance1024(Math.abs("disembodied".hashCode() % 1024));

    final private EntityID disembodiedId;
    final private Examinable.Name name;
    final private ExternalKey externalKey;

    private Optional<IEntityID> locale;
    private final ConcurrentNavigableMap<String, String> notes;

    protected DisembodiedEntity(EntityID disembodiedId, Name name, ExternalKey externalKey, Map<String, String> notes) {
        Preconditions.checkNotNull(disembodiedId, "ID should not be null");
        Preconditions.checkArgument(disembodiedId.entityClass().toString().contains(ENTITY_CLASS_DISEMBODIED),
                "ID should specify that it is %s", ENTITY_CLASS_DISEMBODIED);
        Preconditions.checkNotNull(name, "Name must not be null");
        Preconditions.checkNotNull(externalKey, "External key must not be null");
        this.disembodiedId = disembodiedId;
        this.name = name;
        this.externalKey = externalKey;
        this.locale = Optional.empty();
        this.notes = notes != null ? new ConcurrentSkipListMap<>(notes) : new ConcurrentSkipListMap<>();
    }

    @Override
    public Name name() {
        return this.name;
    }

    @Override
    public Optional<RichOutput> description() {
        RichOutput.Builder sub = RichOutput.builder().setSequenceName("notes")
                .setOnEmpty(Optional.of("No notes found"));
        notes.forEach((k, v) -> {
            sub.addOutput(RichOutput.builder().setSequenceName(k).addString(v).build());
        });
        return Optional.of(RichOutput.builder().setSequenceName(Optional.of(this.name)).addString("This is")
                .addString(this.name.toString()).addString("a disembodied entity. They are also known as")
                .addString(this.externalKey.toString()).addOutput(sub.build()).build());
    }

    @Override
    public Tag tag() {
        return ENTITY_CLASS_DISEMBODIED;
    }

    @Override
    public String content() {
        return this.name.toString();
    }

    @Override
    public IEntityID identifier() {
        return this.disembodiedId;
    }

    @Override
    public Optional<IEntityID> locale() {
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(disembodiedId, name, externalKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof DisembodiedEntity))
            return false;
        DisembodiedEntity<?> other = (DisembodiedEntity<?>) obj;
        return Objects.equals(disembodiedId, other.disembodiedId) && Objects.equals(name, other.name)
                && Objects.equals(externalKey, other.externalKey);
    }

    public static Taggable.Tag getEntityClassDisembodied() {
        return ENTITY_CLASS_DISEMBODIED;
    }

    public static TsidFactory getTsidfactory() {
        return tsidFactory;
    }

    public EntityID getDisembodiedId() {
        return disembodiedId;
    }

    public Examinable.Name getName() {
        return name;
    }

    public ExternalKey getExternalKey() {
        return externalKey;
    }

    public Optional<IEntityID> getLocale() {
        return locale;
    }

    public ConcurrentNavigableMap<String, String> getNotes() {
        return notes;
    }

}
