package com.heyitworks.logback4splunk;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.glassfish.jersey.filter.LoggingFilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * @author maciekr
 */
public class SplunkApiAppender extends AppenderBase<ILoggingEvent> {

    private static final String SOURCE_TYPE = "storm_multi_line";

    private String url = "https://api.splunkstorm.com/1/inputs/http";
    private String username = "x";
    private String token = "YOUR_SPLUNKSTORM_API_TOKEN";
    private String projectId = "YOUR_SPLUNKSTORM_PROJECT_ID";
    private String source = "logback4splunk";
    private String host = "default";

    private int workerMaxThreads = 2;
    private int bufferMaxEvents = 50;
    private boolean debugHttp = false;

    private WebTarget webTarget;
    private Client client;

    private Layout<ILoggingEvent> layout;
    private ThreadPoolExecutor workerThreads;
    private LinkedBlockingQueue<String> eventBuffer;

    @Override
    public void start() {
        super.start();

        this.client = ClientBuilder.newClient();
        this.client.register(new HttpBasicAuthFilter(username, token));
        this.webTarget = client.target(url)
                .queryParam("index", projectId)
                .queryParam("sourcetype", SOURCE_TYPE)
                .queryParam("source", source)
                .queryParam("host", host);
        if (debugHttp)
            this.webTarget.register(new LoggingFilter(Logger.getLogger(LoggingFilter.class.getName()), false));

        this.workerThreads =
                new ThreadPoolExecutor(1, workerMaxThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1000));
        this.eventBuffer = new LinkedBlockingQueue<String>(bufferMaxEvents);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Sweeper(), 60, 120, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        this.client.close();
    }

    @Override
    protected void append(final ILoggingEvent eventObject) {
        Runnable task = new Runnable() {
            String message = layout.doLayout(eventObject);
            @Override
            public void run() {
                doAppend(message);

            }
        };
        try {
            workerThreads.submit(task);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    void doAppend(String msg) {
        if (eventBuffer.offer(msg)) {
            ;
        } else {
            List<String> events = new LinkedList<String>();
            eventBuffer.drainTo(events);
            eventBuffer.add(msg);
            String payload = StringUtils.join(events, "\n");
            webTarget
                    .request()
                    .post(Entity.entity(payload, MediaType.TEXT_PLAIN_TYPE));
        }
    }

    class Sweeper implements Runnable {
        @Override
        public void run() {
            List<String> events = new LinkedList<String>();
            eventBuffer.drainTo(events); 
            if (events.size() > 0) {
            	String payload = StringUtils.join(events, "\n");
            	webTarget
                	.request()
                    	.post(Entity.entity(payload, MediaType.TEXT_PLAIN_TYPE));
	    }
        }
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setBufferMaxEvents(int bufferMaxEvents) {
        this.bufferMaxEvents = bufferMaxEvents;
    }

    public void setWorkerMaxThreads(int workerMaxThreads) {
        this.workerMaxThreads = workerMaxThreads;
    }

    public void setDebugHttp(boolean debugHttp) {
        this.debugHttp = debugHttp;
    }
}
