package org.kompany.overlord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.kompany.overlord.util.PathCache;
import org.kompany.overlord.zookeeper.ResilientZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point into framework functionality.
 * 
 * Should be set up in a single thread.
 * 
 * @author ypai
 * 
 */
public class Sovereign implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(Sovereign.class);

    public static final List<ACL> DEFAULT_ACL_LIST = new ArrayList<ACL>();
    static {
        DEFAULT_ACL_LIST.add(new ACL(ZooDefs.Perms.ALL, new Id("world", "anyone")));
    }

    private ZkClient zkClient;

    private Map<String, ServiceWrapper> serviceMap = new HashMap<String, ServiceWrapper>();

    private Map<String, Future<?>> futureMap = new HashMap<String, Future<?>>();

    private PathScheme pathScheme = new DefaultPathScheme("/sovereign/user", "/sovereign/internal");

    private PathCache pathCache;

    private ScheduledExecutorService executorService;

    private volatile boolean started = false;
    private volatile boolean shutdown = false;

    private int threadPoolSize = 3;

    private Thread adminThread = null;

    /** List to ensure Watcher(s) are called in a specific order */
    private List<Watcher> watcherList = new ArrayList<Watcher>();

    public Sovereign() {
    }

    public Sovereign(String zkConnectString, int zkSessionTimeout) {
        this(zkConnectString, zkSessionTimeout, 1024, 8);
    }

    public Sovereign(String zkConnectString, int zkSessionTimeout, int pathCacheSize, int pathCacheConcurrencyLevel) {
        this();
        try {
            zkClient = new ResilientZooKeeper(zkConnectString, zkSessionTimeout);
        } catch (IOException e) {
            throw new IllegalStateException("Fatal error:  could not initialize Zookeeper client!");
        }

        this.pathCache = new PathCache(pathCacheSize, pathCacheConcurrencyLevel, zkClient);
    }

    @Override
    public void process(WatchedEvent event) {
        // log if DEBUG
        if (logger.isDebugEnabled()) {
            logger.debug("***** Received ZooKeeper Event:  {}",
                    ReflectionToStringBuilder.toString(event, ToStringStyle.DEFAULT_STYLE));

        }

        for (Watcher watcher : watcherList) {
            watcher.process(event);
        }
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        if (started) {
            throw new IllegalStateException("Cannot set zkClient once started!");
        }
        this.zkClient = zkClient;
    }

    public PathScheme getPathScheme() {
        return pathScheme;
    }

    public void setPathScheme(PathScheme pathScheme) {
        if (started) {
            throw new IllegalStateException("Cannot set pathScheme once started!");
        }
        this.pathScheme = pathScheme;
    }

    public PathCache getPathCache() {
        return pathCache;
    }

    public void setPathCache(PathCache pathCache) {
        if (started) {
            throw new IllegalStateException("Cannot set pathCache once started!");
        }
        this.pathCache = pathCache;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        if (started) {
            throw new IllegalStateException("Cannot set threadPoolSize once started!");
        }
        this.threadPoolSize = threadPoolSize;
    }

    // public boolean isStarted() {
    // return started;
    // }
    //
    // public boolean isShutdown() {
    // return shutdown;
    // }

    public Service getService(String serviceName) {
        ServiceWrapper serviceWrapper = this.serviceMap.get(serviceName);
        return serviceWrapper.getService();
    }

    void register(String serviceName, Service service) {
        throwExceptionIfNotOkayToRegister();

        logger.info("Registering service:  serviceName={}", serviceName);

        // set with path scheme
        service.setPathScheme(pathScheme);
        service.setZkClient(zkClient);
        service.setPathCache(pathCache);
        service.init();

        // add to zkClient's list of watchers if Watcher interface is
        // implemented
        if (service instanceof Watcher) {
            logger.info("Adding as ZooKeeper watcher:  serviceName={}", serviceName);
            watcherList.add((Watcher) service);
        }

        serviceMap.put(serviceName, new ServiceWrapper(service));
    }

    public synchronized void registerServices(Map<String, Service> serviceMap) {
        throwExceptionIfNotOkayToRegister();

        for (String serviceName : serviceMap.keySet()) {
            register(serviceName, serviceMap.get(serviceName));
        }
    }

    void throwExceptionIfNotOkayToRegister() {
        if (started) {
            throw new IllegalStateException("Cannot register services once started!");
        }
        if (zkClient == null || pathCache == null) {
            throw new IllegalStateException(
                    "Cannot register services before zkClient and pathCache has been populated!");
        }
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;

        logger.info("START:  begin");

        /** create graceful shutdown hook **/
        logger.info("START:  add shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Sovereign.this.stop();
            }
        });

        /** watcher set-up **/
        // register self as watcher
        this.zkClient.register(this);

        // pathCache should be first in list
        this.watcherList.add(0, pathCache);

        /** init executor **/
        logger.info("START:  initializing executor");
        this.executorService = this.createExecutorService();

        /** start services running **/
        logger.info("START:  schedule service tasks");
        for (String serviceName : serviceMap.keySet()) {
            ServiceWrapper serviceWrapper = serviceMap.get(serviceName);
            logger.debug("Checking service:  {}", serviceWrapper.getService().getClass().getName());

            // execute if not a continuously running service and not shutdown
            if (!this.shutdown && serviceWrapper.isSubmittable()) {
                logger.debug("Submitting service:  {}", serviceWrapper.getService().getClass().getName());
                Future<?> future = executorService.scheduleWithFixedDelay(serviceWrapper,
                        serviceWrapper.getIntervalMillis(), serviceWrapper.getIntervalMillis(), TimeUnit.MILLISECONDS);
                futureMap.put(serviceName, future);
            }// if
        }// for

        /** start main thread **/
        logger.info("START:  start admin thread");
        adminThread = this.createAdminThread();
        adminThread.start();

        logger.info("START:  done");
    }

    public synchronized void stop() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        logger.info("SHUTDOWN:  begin");

        /** cancel all futures **/
        logger.info("SHUTDOWN:  cancelling scheduled service tasks");
        for (Future<?> future : futureMap.values()) {
            future.cancel(false);
        }

        /** stop admin thread **/
        logger.info("SHUTDOWN:  shutting down admin thread");
        try {
            if (adminThread != null) {
                adminThread.join();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted during shutdown:  " + e, e);
        }

        /** stop executor **/
        logger.info("SHUTDOWN:  shutting down executor");
        executorService.shutdown();

        /** clean up services **/
        logger.info("SHUTDOWN:  cleaning up services");
        for (ServiceWrapper serviceWrapper : serviceMap.values()) {
            serviceWrapper.getService().destroy();
        }

        /** clean up zk client **/
        logger.info("SHUTDOWN:  closing Zookeeper client");
        this.zkClient.close();

        logger.info("SHUTDOWN:  done");
    }

    /**
     * Creates and appropriately sizes executor service for services configured.
     * 
     * @return
     */
    private ScheduledExecutorService createExecutorService() {
        // get number of continuously running services
        int continuouslyRunningCount = 0;
        for (ServiceWrapper serviceWrapper : serviceMap.values()) {
            if (serviceWrapper.isActiveService() && serviceWrapper.isAlwaysActive()) {
                continuouslyRunningCount++;
            }
        }
        return new ScheduledThreadPoolExecutor(continuouslyRunningCount + this.getThreadPoolSize());
    }

    /**
     * Creates the thread that submits the services for regular execution.
     * 
     * @return
     */
    private Thread createAdminThread() {
        Thread adminThread = new Thread() {
            @Override
            public void run() {
                while (!shutdown) {
                    // TODO: perform any framework maintenance?

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.warn("Interrupted while in mainThread:  " + e, e);
                    }
                }// while
            }// run()
        };
        adminThread.setName(this.getClass().getSimpleName() + "." + "admin");
        adminThread.setDaemon(true);
        return adminThread;
    }

    /**
     * Convenience wrapper providing methods for interpreting service metadata.
     * 
     * @author ypai
     * 
     */
    private static class ServiceWrapper implements Runnable {
        private Service service;
        private boolean running = false;

        public ServiceWrapper(Service service) {
            this.service = service;
        }

        public Service getService() {
            return service;
        }

        public boolean isActiveService() {
            return service instanceof ActiveService;
        }

        public boolean isSubmittable() {
            return isActiveService() && !isRunning();
        }

        public synchronized boolean isAlwaysActive() {
            return ((ActiveService) this.service).getExecutionIntervalMillis() < 1;
        }

        public synchronized boolean isRunning() {
            return running;
        }

        public long getIntervalMillis() {
            return ((ActiveService) service).getExecutionIntervalMillis();
        }

        @Override
        public synchronized void run() {
            this.running = true;

            logger.debug("Calling {}.perform()", service.getClass().getName());
            ((ActiveService) this.service).perform();

            this.running = false;
        }

    }
}
