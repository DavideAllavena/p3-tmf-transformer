package eu.fusepool.p3.transformer.tmf;

import eu.fusepool.p3.transformer.AsyncTransformer;
import eu.fusepool.p3.transformer.HttpRequestEntity;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * AsyncSedTransformer is an asynchronous version of SedTransformer which
 * queues incoming requests and processes them serially by means of a single
 * thread.
 */
public class AsyncTMFTransformer extends TMFTransformer implements AsyncTransformer {

    private final static int MAX_REQUEST_BACKLOG = 100;

    private final LinkedBlockingQueue<String> fQueue = new LinkedBlockingQueue<>(MAX_REQUEST_BACKLOG);

    private final ConcurrentHashMap<String, HttpRequestEntity> fActive = new ConcurrentHashMap<>();

    private volatile CallBackHandler fCallback;

    @Override
    public void transform(HttpRequestEntity entity, String requestId) throws IOException {

        if (!fQueue.offer(requestId)) {
            throw new eu.fusepool.p3.transformer.tmf.TooManyRequests("Too many requests on backlog.");
        }

        // This should generally not be a problem as we don't expect requestId
        // collisions.
        fActive.put(requestId, entity);
    }

    @Override
    public void activate(CallBackHandler callBackHandler) {
        fCallback = callBackHandler;
        new Thread(new SimpleExecutor()).start();
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }


    @Override
    public boolean isActive(String requestId) {
        return fActive.containsKey(requestId);
    }


    /**
     * Simple single-threaded executor that processes queued requests.
     */
    class SimpleExecutor implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    processRequest(fQueue.poll(Long.MAX_VALUE, TimeUnit.DAYS));
                }
            } catch (InterruptedException ex) {
                // Just restores interruption state.
                Thread.currentThread().interrupt();
            }
        }

        public void processRequest(String requestId) {

            HttpRequestEntity entity = fActive.get(requestId);
            if (entity == null) {
                fCallback.reportException(requestId,
                        new IllegalStateException("Request " + requestId + " is no longer active. " +
                                "Maybe there was an ID collision?"));
                return;
            }

            try {
                fCallback.responseAvailable(requestId, transform(entity));
            } catch (Exception ex) {
                fCallback.reportException(requestId, ex);
            }
        }
    }

}

