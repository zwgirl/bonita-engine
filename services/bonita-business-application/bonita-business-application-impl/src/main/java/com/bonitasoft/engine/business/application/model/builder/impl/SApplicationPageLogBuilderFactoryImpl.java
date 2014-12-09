/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.business.application.model.builder.impl;

import org.bonitasoft.engine.queriablelogger.model.builder.impl.CRUDELogBuilderFactory;

import com.bonitasoft.engine.business.application.model.builder.SApplicationLogBuilderFactory;


/**
 * @author Elias Ricken de Medeiros
 *
 */
public class SApplicationPageLogBuilderFactoryImpl extends CRUDELogBuilderFactory implements SApplicationLogBuilderFactory {

    public static final int APPLICATION_PAGE_INDEX = 1;

    public static final String APPLICATION_PAGE_INDEX_NAME = "numericIndex2";

    @Override
    public String getObjectIdKey() {
        return APPLICATION_PAGE_INDEX_NAME;
    }

}
