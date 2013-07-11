package com.heyitworks.logback4splunk;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Common Raw TCP logic shared by all appenders/handlers
 *
 * @author Damien Dallimore damien@dtdsoftware.com
 */

public class SplunkRawTCPInput extends SplunkInput {

    // connection props
    private String host = "";
    private int port;

    // streaming objects
    private Socket streamSocket = null;
    private OutputStream ostream;
    private Writer writerOut = null;

    //maciekr - need to have something like that to effectively protect OutputStreamWriter from concurrent modification as there is no protection of any sort in this code
    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1000), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "SplunkRawTCPInput-OutStream-Serializer");
        }
    });


    /**
     * Create a SplunkRawTCPInput object to send events to Splunk via Raw TCP
     *
     * @param host REST endppoint host
     * @param port REST endpoint port
     * @throws Exception
     */
    public SplunkRawTCPInput(String host, int port) throws Exception {

        this.host = host;
        this.port = port;

        openStream();

    }

    /**
     * open the stream
     */
    private void openStream() throws Exception {
        streamSocket = new Socket(host, port);
        if (streamSocket.isConnected()) {
            ostream = streamSocket.getOutputStream();
            writerOut = new OutputStreamWriter(ostream, "UTF8");
        }
    }

    /**
     * close the stream
     */
    public void closeStream() {
        try {

            if (writerOut != null) {
                writerOut.flush();
                writerOut.close();
                if (streamSocket != null)
                    streamSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * send an event via stream
     *
     * @param message
     */
    public void streamEvent(final String message) {
        try {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    String currentMessage = message;
                    try {

                        if (writerOut != null) {

                            //send the message
                            writerOut.write(currentMessage + "\n");

                            //flush the queue
                            while (queueContainsEvents()) {
                                String messageOffQueue = dequeue();
                                currentMessage = messageOffQueue;
                                writerOut.write(currentMessage + "\n");
                            }
                            writerOut.flush();
                        }

                    } catch (IOException e) {

                        //something went wrong , put message on the queue for retry
                        enqueue(currentMessage);
                        try {
                            closeStream();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                        try {
                            openStream();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
        }
    }
}
