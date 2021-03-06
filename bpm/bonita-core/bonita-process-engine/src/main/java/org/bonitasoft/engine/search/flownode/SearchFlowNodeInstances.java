/**
 * Copyright (C) 2019 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.search.flownode;

import java.util.List;

import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.api.FlowNodeInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.search.AbstractSearchEntity;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.descriptor.SearchFlowNodeInstanceDescriptor;
import org.bonitasoft.engine.service.ModelConvertor;

/**
 * @author Elias Ricken de Medeiros
 */
public class SearchFlowNodeInstances extends AbstractSearchEntity<FlowNodeInstance, SFlowNodeInstance> {

    private final FlowNodeInstanceService flowNodeInstanceService;

    private final FlowNodeStateManager flowNodeStateManager;

    public SearchFlowNodeInstances(final FlowNodeInstanceService flowNodeInstanceService,
            final FlowNodeStateManager flowNodeStateManager,
            final SearchFlowNodeInstanceDescriptor searchDescriptor, final SearchOptions searchOptions) {
        super(searchDescriptor, searchOptions);
        this.flowNodeInstanceService = flowNodeInstanceService;
        this.flowNodeStateManager = flowNodeStateManager;
    }

    @Override
    public long executeCount(final QueryOptions searchOptions) throws SBonitaReadException {
        return flowNodeInstanceService.getNumberOfFlowNodeInstances(SFlowNodeInstance.class, searchOptions);
    }

    @Override
    public List<SFlowNodeInstance> executeSearch(final QueryOptions searchOptions) throws SBonitaReadException {
        return flowNodeInstanceService.searchFlowNodeInstances(SFlowNodeInstance.class, searchOptions);
    }

    @Override
    public List<FlowNodeInstance> convertToClientObjects(final List<SFlowNodeInstance> serverObjects) {
        return ModelConvertor.toFlowNodeInstances(serverObjects, flowNodeStateManager);
    }

}
