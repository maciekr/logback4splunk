package com.heyitworks.logback4splunk;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

/**
 * LogBack Appender for sending events to Splunk via Raw TCP
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRawTCPAppender extends AppenderBase<ILoggingEvent> {

	private String host;
	private int port;

	private SplunkRawTCPInput sri;
	private Layout<ILoggingEvent> layout;

	@Override
	protected void append(ILoggingEvent event) {
        if (sri != null) {
			String formatted = layout.doLayout(event);
            sri.streamEvent(formatted);
		}
	}

	@Override
	public void start() {
        if (this.layout == null) {
			addError("No layout set for the appender named [" + name + "].");
			return;
		}
		if (sri == null) {
			try {
				sri = new SplunkRawTCPInput(host, port);
			} catch (Exception e) {
				addError("Couldn't establish Raw TCP connection for SplunkRawTCPAppender named \""
						+ this.name + "\".");
			}
		}
        super.start();
	}

	/**
	 * Clean up resources
	 */
	@Override
	public void stop() {
		if (sri != null) {
			try {
				sri.closeStream();
				sri = null;
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				sri = null;
			}
		}
		super.stop();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Layout<ILoggingEvent> getLayout() {
		return layout;
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		this.layout = layout;
	}

}
