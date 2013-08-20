/*******************************************************************************
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.classloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bonitasoft.engine.classloader.ClassLoaderException;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

/**
 * 
 * @author Baptiste Mesta
 * 
 */
public class ClusteredClassLoaderService implements ClassLoaderService {

    private static final String EXECUTOR_NAME = "default";

    /**
     * static in order to be accessed by the RefreshClassLoaderTask that is
     * serialized and given to hazelcast
     */
    static HazelcastInstance hazelcastInstance;

    static ClassLoaderService classLoaderService;

    static TechnicalLoggerService loggerService;

    @SuppressWarnings("static-access")
    public ClusteredClassLoaderService(HazelcastInstance hazelcastInstance,
            ClassLoaderService classLoaderService,
            TechnicalLoggerService loggerService) {
        this.hazelcastInstance = hazelcastInstance;
        this.classLoaderService = classLoaderService;
        this.loggerService = loggerService;
    }

    @Override
    public ClassLoader getGlobalClassLoader() throws ClassLoaderException {
        return classLoaderService.getGlobalClassLoader();
    }

    @Override
    public String getGlobalClassLoaderType() {
        return classLoaderService.getGlobalClassLoaderType();
    }

    @Override
    public long getGlobalClassLoaderId() {
        return classLoaderService.getGlobalClassLoaderId();
    }

    @Override
    public ClassLoader getLocalClassLoader(String type, long id)
            throws ClassLoaderException {
        return classLoaderService.getLocalClassLoader(type, id);
    }

    @Override
    public void removeLocalClassLoader(String type, long id) {
        classLoaderService.removeLocalClassLoader(type, id);

    }

    @Override
    public void removeAllLocalClassLoaders(String type) {
        classLoaderService.removeAllLocalClassLoaders(type);

    }

    @Override
    public void refreshGlobalClassLoader(Map<String, byte[]> resources)
            throws ClassLoaderException {

        // we use the executor service to refresh classloader on all nodes

        RefreshClassLoaderTask refreshClassLoaderTask = new RefreshClassLoaderTask(
                resources);

        executeRefreshOnCluster(null, -1, refreshClassLoaderTask);

    }

    @Override
    public void refreshLocalClassLoader(String type, long id,
            Map<String, byte[]> resources) throws ClassLoaderException {

        // we use the executor service to refresh classloader on all nodes

        RefreshClassLoaderTask refreshClassLoaderTask = new RefreshClassLoaderTask(
                type, id, resources);

        executeRefreshOnCluster(type, id, refreshClassLoaderTask);
    }

    private void executeRefreshOnCluster(String type, long id,
            RefreshClassLoaderTask refreshClassLoaderTask)
            throws ClassLoaderException {
        long before = System.currentTimeMillis();
        Map<Member, Future<TaskStatus>> submitToAllMembers = hazelcastInstance.getExecutorService(EXECUTOR_NAME).submitToAllMembers(refreshClassLoaderTask);
        // wait for result;
        try {
            List<TaskStatus> results = new ArrayList<TaskStatus>(submitToAllMembers.size());
            for (Entry<Member, Future<TaskStatus>> result : submitToAllMembers.entrySet()) {
                results.add(result.getValue().get());
            }
            long after = System.currentTimeMillis();
            loggerService.log(ClusteredClassLoaderService.class,
                    TechnicalLogSeverity.INFO, "Refreshing classloader "
                            + (type != null ? type + " " + id : "global")
                            + " of all nodes. took" + (after - before));
            for (TaskStatus result : results) {
                loggerService.log(ClusteredClassLoaderService.class,
                        TechnicalLogSeverity.INFO, result.getMessage());
                if (result.isError()) {
                    loggerService.log(ClusteredClassLoaderService.class,
                            TechnicalLogSeverity.DEBUG, result.getThrowable());
                    throw new ClassLoaderException(result.getThrowable());
                }
            }
        } catch (ExecutionException e) {
            // exception in a node
            throw new ClassLoaderException(e);
        } catch (InterruptedException e) {
            // TIMEOUT
            throw new ClassLoaderException(e);
        }
    }

}
