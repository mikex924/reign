package org.kompany.sovereign.examples;

import java.util.Map;

import org.kompany.sovereign.Sovereign;
import org.kompany.sovereign.messaging.MessagingService;
import org.kompany.sovereign.messaging.ResponseMessage;
import org.kompany.sovereign.messaging.SimpleRequestMessage;
import org.kompany.sovereign.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrate basic usage.
 * 
 * @author ypai
 * 
 */
public class MessagingExample {

    private static final Logger logger = LoggerFactory.getLogger(MessagingExample.class);

    public static void main(String[] args) throws Exception {
        /** init and start sovereign using builder **/
        Sovereign sovereign = Sovereign.builder().zkClient("localhost:2181", 15000).pathCache(1024, 8)
                .allCoreServices().build();
        sovereign.start();

        /** messaging example **/
        messagingExample(sovereign);

        /** sleep to allow examples to run for a bit **/
        Thread.sleep(120000);

        /** shutdown sovereign **/
        sovereign.stop();

        /** sleep a bit to observe observer callbacks **/
        Thread.sleep(10000);
    }

    public static void messagingExample(Sovereign sovereign) throws Exception {
        PresenceService presenceService = sovereign.getService("presence");
        presenceService.waitUntilAvailable("sovereign", "messaging", 30000);

        MessagingService messagingService = sovereign.getService("messaging");
        Map<String, ResponseMessage> responseMap = messagingService.sendMessage("sovereign", "messaging",
                new SimpleRequestMessage("presence", "/"));

        logger.info("responseMap={}", responseMap);
    }
}