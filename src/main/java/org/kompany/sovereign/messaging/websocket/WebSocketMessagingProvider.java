package org.kompany.sovereign.messaging.websocket;

import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kompany.sovereign.ServiceDirectory;
import org.kompany.sovereign.UnexpectedSovereignException;
import org.kompany.sovereign.messaging.DefaultMessageProtocol;
import org.kompany.sovereign.messaging.MessageProtocol;
import org.kompany.sovereign.messaging.MessagingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ypai
 * 
 */
public class WebSocketMessagingProvider implements MessagingProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessagingProvider.class);

    private int port;

    private WebSocketServer server;

    private volatile boolean shutdown = true;

    private ExecutorService executorService;

    private ServiceDirectory serviceDirectory;

    private MessageProtocol messageProtocol;

    private final ConcurrentMap<String, WebSocketClient> clientMap = new ConcurrentHashMap<String, WebSocketClient>(32,
            0.9f, 2);

    @Override
    public String sendMessage(String hostOrIpAddress, int port, String message) {
        String endpointUri = "ws://" + hostOrIpAddress + ":" + port;
        WebSocketClient client = getClient(endpointUri);
        return client.write(message);
    }

    @Override
    public byte[] sendMessage(String hostOrIpAddress, int port, byte[] message) {
        String endpointUri = "ws://" + hostOrIpAddress + ":" + port;
        WebSocketClient client = getClient(endpointUri);
        return client.write(message);
    }

    @Override
    public MessageProtocol getMessageProtocol() {
        return messageProtocol;
    }

    @Override
    public void setMessageProtocol(MessageProtocol messageProtocol) {
        this.messageProtocol = messageProtocol;
    }

    public ServiceDirectory getServiceDirectory() {
        return serviceDirectory;
    }

    @Override
    public void setServiceDirectory(ServiceDirectory serviceDirectory) {
        this.serviceDirectory = serviceDirectory;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;

    }

    WebSocketClient getClient(String endpointUri) {
        WebSocketClient client = clientMap.get(endpointUri);
        if (client == null) {
            try {
                WebSocketClient newClient = new WebSocketClient(endpointUri);
                client = clientMap.putIfAbsent(endpointUri, newClient);
                if (client == null) {
                    client = newClient;
                    newClient.connect();
                }
            } catch (URISyntaxException e) {
                throw new UnexpectedSovereignException(e);
            }
        }// if
        return client;
    }

    @Override
    public synchronized void start() {
        if (!shutdown) {
            return;
        }

        if (messageProtocol == null) {
            logger.info("START:  using default message protocol");
            messageProtocol = new DefaultMessageProtocol();
        }

        logger.info("START:  starting websockets server");
        this.server = new WebSocketServer(port, serviceDirectory, messageProtocol);
        server.start();

        logger.info("START:  initializing executor");
        this.executorService = Executors.newFixedThreadPool(2);

        shutdown = false;

    }

    @Override
    public synchronized void stop() {
        if (shutdown) {
            return;
        }

        logger.info("STOP:  shutting down websockets server");
        server.stop();
        shutdown = true;

        logger.info("STOP:  shutting down executor");
        executorService.shutdown();

        this.shutdown = true;
    }

}