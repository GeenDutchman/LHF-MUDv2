package com.geendutchman.lhf_mudv2.execution.commandline;

import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.CreatureFromContext;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.EntityFromContext;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.EntityIDConverter;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.ItemFromContext;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.SenderCreatureItemsOnly;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.SenderRoomCreaturesOnly;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.SenderRoomItemsOnly;
import com.google.common.base.Preconditions;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

final class InjectedFactory implements IFactory {
    private final IFactory subfactory;
    private final MessageContext context;
    private final CreatureFromContext cfc;
    private final EntityFromContext efc;
    private final ItemFromContext ifc;
    private final SenderCreatureItemsOnly scio;
    private final SenderRoomCreaturesOnly srco;
    private final SenderRoomItemsOnly srio;
    private final EntityIDConverter eic;

    protected InjectedFactory(IFactory subFactory, MessageContext context) {
        Preconditions.checkNotNull(subFactory, "subfactory must not be null");
        Preconditions.checkNotNull(context, "context must not be null");
        this.subfactory = subFactory;
        this.context = context;
        this.cfc = new CreatureFromContext(context);
        this.efc = new EntityFromContext(context);
        this.ifc = new ItemFromContext(context);
        this.scio = new SenderCreatureItemsOnly(context);
        this.srco = new SenderRoomCreaturesOnly(context);
        this.srio = new SenderRoomItemsOnly(context);
        this.eic = new EntityIDConverter();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            if (context != null && context.getClass().isAssignableFrom(cls)) {
                K result = (K) context;
                return result;
            }
            if (cfc.getClass().isAssignableFrom(cls)) {
                return (K) cfc;
            } else if (efc.getClass().isAssignableFrom(cls)) {
                return (K) efc;
            } else if (ifc.getClass().isAssignableFrom(cls)) {
                return (K) ifc;
            } else if (scio.getClass().isAssignableFrom(cls)) {
                return (K) scio;
            } else if (srco.getClass().isAssignableFrom(cls)) {
                return (K) srco;
            } else if (srio.getClass().isAssignableFrom(cls)) {
                return (K) srio;
            } else if (eic.getClass().isAssignableFrom(cls)) {
                return (K) eic;
            }
            return subfactory.create(cls);
        } catch (Exception e) {
            return CommandLine.defaultFactory().create(cls);
        }
    }

    public IFactory getSubfactory() {
        return subfactory;
    }

    public MessageContext getContext() {
        return context;
    }

    public CreatureFromContext getCfc() {
        return cfc;
    }

    public EntityFromContext getEfc() {
        return efc;
    }

    public ItemFromContext getIfc() {
        return ifc;
    }

    public SenderCreatureItemsOnly getScio() {
        return scio;
    }

    public SenderRoomCreaturesOnly getSrco() {
        return srco;
    }

    public SenderRoomItemsOnly getSrio() {
        return srio;
    }

    public EntityIDConverter getEic() {
        return eic;
    }

}
