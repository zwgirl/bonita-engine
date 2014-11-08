/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.api.impl.application;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SObjectModificationException;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.search.SearchResult;

import com.bonitasoft.engine.api.impl.convertor.ApplicationMenuConvertor;
import com.bonitasoft.engine.api.impl.transaction.application.SearchApplicationMenus;
import com.bonitasoft.engine.api.impl.validator.ApplicationMenuCreatorValidator;
import com.bonitasoft.engine.business.application.ApplicationMenu;
import com.bonitasoft.engine.business.application.ApplicationMenuCreator;
import com.bonitasoft.engine.business.application.ApplicationMenuNotFoundException;
import com.bonitasoft.engine.business.application.ApplicationMenuUpdater;
import com.bonitasoft.engine.business.application.ApplicationService;
import com.bonitasoft.engine.business.application.model.SApplicationMenu;
import com.bonitasoft.engine.business.application.model.builder.SApplicationUpdateBuilderFactory;
import com.bonitasoft.engine.service.TenantServiceAccessor;

/**
 * @author Elias Ricken de Medeiros
 */
public class ApplicationMenuAPIDelegate {

    private final ApplicationMenuConvertor convertor;
    private final ApplicationService applicationService;
    private final SearchApplicationMenus searchApplicationMenus;
    private final ApplicationMenuCreatorValidator creatorValidator;
    private final long loggedUserId;

    public ApplicationMenuAPIDelegate(final TenantServiceAccessor accessor, final ApplicationMenuConvertor convertor,
            final SearchApplicationMenus searchApplicationMenus, final ApplicationMenuCreatorValidator creatorValidator, final long loggedUserId) {
        this.searchApplicationMenus = searchApplicationMenus;
        this.creatorValidator = creatorValidator;
        this.loggedUserId = loggedUserId;
        applicationService = accessor.getApplicationService();
        this.convertor = convertor;
    }

    public ApplicationMenu createApplicationMenu(final ApplicationMenuCreator applicationMenuCreator) throws CreationException {
        try {
            if (!creatorValidator.isValid(applicationMenuCreator)) {
                throw new CreationException("The ApplicationMenuCreator is invalid. Problems: " + creatorValidator.getProblems());
            }
            final int index = applicationService.getNextAvailableIndex(applicationMenuCreator.getParentId());
            final SApplicationMenu sApplicationMenu = applicationService.createApplicationMenu(convertor.buildSApplicationMenu(applicationMenuCreator, index));
            applicationService.updateApplication(sApplicationMenu.getApplicationId(), BuilderFactory.get(SApplicationUpdateBuilderFactory.class)
                    .createNewInstance(loggedUserId).done());
            return convertor.toApplicationMenu(sApplicationMenu);
        } catch (final SBonitaException e) {
            throw new CreationException(e);
        }
    }

    public ApplicationMenu updateApplicationMenu(final long applicationMenuId, final ApplicationMenuUpdater updater) throws ApplicationMenuNotFoundException,
            UpdateException {
        final EntityUpdateDescriptor updateDescriptor = convertor.toApplicationMenuUpdateDescriptor(updater);
        try {
            final SApplicationMenu sApplicationMenu = applicationService.updateApplicationMenu(applicationMenuId, updateDescriptor);
            applicationService.updateApplication(sApplicationMenu.getApplicationId(), BuilderFactory.get(SApplicationUpdateBuilderFactory.class)
                    .createNewInstance(loggedUserId).done());
            return convertor.toApplicationMenu(sApplicationMenu);
        } catch (final SObjectModificationException e) {
            throw new UpdateException(e);
        } catch (final SObjectNotFoundException e) {
            throw new ApplicationMenuNotFoundException(e.getMessage());
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    public ApplicationMenu getApplicationMenu(final long applicationMenuId) throws ApplicationMenuNotFoundException {
        try {
            final SApplicationMenu sApplicationMenu = applicationService.getApplicationMenu(applicationMenuId);
            return convertor.toApplicationMenu(sApplicationMenu);
        } catch (final SObjectNotFoundException e) {
            throw new ApplicationMenuNotFoundException(e.getMessage());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    public void deleteApplicationMenu(final long applicationMenuId) throws DeletionException {
        try {
            final SApplicationMenu deletedApplicationMenu = applicationService.deleteApplicationMenu(applicationMenuId);
            applicationService.updateApplication(deletedApplicationMenu.getApplicationId(), BuilderFactory.get(SApplicationUpdateBuilderFactory.class)
                    .createNewInstance(loggedUserId).done());
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    public SearchResult<ApplicationMenu> searchApplicationMenus() throws SearchException {
        try {
            searchApplicationMenus.execute();
            return searchApplicationMenus.getResult();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
    }

}
