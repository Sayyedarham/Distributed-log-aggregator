package com.logaggregator.gateway.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class EventBus {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    private final Map<EventType, List<EventListener>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public EventBus() {
        this.executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "eventbus-worker-" + (++counter));
                t.setDaemon(true);
                return t;
            }
        });
    }

    public void subscribe(EventType type, EventListener listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unsubscribe(EventType type, EventListener listener) {
        List<EventListener> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void publish(SystemEvent event) {
        List<EventListener> list = listeners.get(event.getType());
        if (list == null || list.isEmpty()) {
            return;
        }

        executor.submit(() -> {
            for (EventListener listener : list) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.error("Error in event listener handling {}: {}", event.getType(), e.getMessage(), e);
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

