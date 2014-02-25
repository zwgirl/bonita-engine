/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.bdm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.engine.commons.io.IOUtil;
import org.xml.sax.SAXException;

import com.bonitasoft.engine.business.data.SBusinessDataRepositoryDeploymentException;
import com.bonitasoft.engine.business.data.impl.PersistenceUnitBuilder;
import com.bonitasoft.engine.compiler.CompilationException;
import com.bonitasoft.engine.compiler.JDTCompiler;
import com.bonitasoft.engine.io.IOUtils;
import com.sun.codemodel.JClassAlreadyExistsException;

/**
 * @author Matthieu Chaffotte
 */
public class BDMJarBuilder {

    private final BDMCompiler compiler;

    public BDMJarBuilder(final BDMCompiler compiler) {
        this.compiler = compiler;
    }

    public byte[] build(final byte[] bomZip) throws SBusinessDataRepositoryDeploymentException {
        try {
            final BusinessObjectModel bom = getBOM(bomZip);
            final File tmpBDMDirectory = createBDMTmpDir();
            try {
                generateJavaFiles(bom, tmpBDMDirectory);
                compiler.compile(tmpBDMDirectory);
                addPersistenceFile(tmpBDMDirectory, bom);
                return generateJar(tmpBDMDirectory);
            } finally {
                FileUtils.deleteDirectory(tmpBDMDirectory);
            }
        } catch (final Exception e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        }
    }

    protected BusinessObjectModel getBOM(final byte[] bomZip) throws IOException, JAXBException, SAXException {
        final BusinessObjectModelConverter converter = new BusinessObjectModelConverter();
        return converter.unzip(bomZip);
    }

    protected File createBDMTmpDir() throws IOException {
        return IOUtils.createTempDirectory("bdm");
    }

    protected byte[] generateJar(final File directory) throws IOException {
        final Collection<File> files = FileUtils.listFiles(directory, new String[] { "class", "xml" }, true);
        final Map<String, byte[]> resources = new HashMap<String, byte[]>();
        for (final File file : files) {
            final String relativeName = directory.toURI().relativize(file.toURI()).getPath();
            final byte[] content = FileUtils.readFileToByteArray(file);
            resources.put(relativeName, content);
        }
        return IOUtil.generateJar(resources);
    }

    protected void generateJavaFiles(final BusinessObjectModel bom, final File directory) throws IOException, JClassAlreadyExistsException {
        final BDMCodeGenerator codeGenerator = new BDMCodeGenerator(bom);
        codeGenerator.generate(directory);
    }

    protected void addPersistenceFile(final File directory, final BusinessObjectModel bom) throws IOException, TransformerException,
            ParserConfigurationException, SAXException {
        final List<BusinessObject> entities = bom.getEntities();
        final PersistenceUnitBuilder builder = new PersistenceUnitBuilder();
        for (final BusinessObject businessObject : entities) {
            builder.addClass(businessObject.getQualifiedName());
        }
        final Document document = builder.done();
        final File metaInf = IOUtils.createSubDirectory(directory, "META-INF");
        IOUtils.saveDocument(document, new File(metaInf, "persistence.xml"));
    }

}
