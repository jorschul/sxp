/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntriesFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.DomainFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class DomainFilterListener extends ListListener<SxpDomain, DomainFilters, DomainFilter> {

    public DomainFilterListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, DomainFilters.class);
    }

    @Override
    protected void handleOperational(DataObjectModification<DomainFilter> c, InstanceIdentifier<SxpDomain> identifier) {
        final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue(),
                domain = identifier.firstKeyOf(SxpDomain.class).getDomainName();
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null) {
            LOG.error("Operational Modification {} {} could not get SXPNode {}", getClass(), c.getModificationType(),
                    nodeId);
            return;
        }
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null) {
                    sxpNode.addFilterToDomain(domain, c.getDataAfter());
                    break;
                } else if (c.getDataAfter() == null) {
                    sxpNode.removeFilterFromDomain(domain, c.getDataBefore().getFilterSpecific());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkDifference(c, FilterEntriesFields::getFilterEntries)) {
                    sxpNode.removeFilterFromDomain(domain, c.getDataBefore().getFilterSpecific());
                    sxpNode.addFilterToDomain(domain, c.getDataAfter());
                } else if (checkDifference(c, SxpDomainFilterFields::getDomains)) {
                    //TODO
                }
                break;
            case DELETE:
                sxpNode.removeFilterFromDomain(domain, c.getDataBefore().getFilterSpecific());
                break;
        }
    }

    @Override protected InstanceIdentifier<DomainFilter> getIdentifier(DomainFilter d,
            InstanceIdentifier<SxpDomain> parentIdentifier) {
        return parentIdentifier.child(DomainFilters.class)
                .child(DomainFilter.class, new DomainFilterKey(d.getFilterSpecific()));
    }
}
