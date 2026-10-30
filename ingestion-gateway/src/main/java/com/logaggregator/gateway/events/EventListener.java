package com.logaggregator.gateway.events;

@FunctionalInterface
public interface EventListener {
    void onEvent(SystemEvent event);
}

