package com.heyitworks.logback4splunk;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: mrakowicz
 * Date: 08/05/2013
 * Time: 14:23
 * To change this template use File | Settings | File Templates.
 */
public class ATest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ATest.class);

   @Test
   public void doSomeLogging() {
       MDC.put("correlationId", UUID.randomUUID().toString().replaceAll("-", ""));
       for (int i=0;i<100;i++) {
           LOGGER.info("log entry " + i);
       }
   }
}
