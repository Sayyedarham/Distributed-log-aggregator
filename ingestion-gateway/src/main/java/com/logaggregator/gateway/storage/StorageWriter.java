package com.logaggregator.gateway.storage;

import com.logaggregator.gateway.model.MetricPayload;

public interface StorageWriter {

    void write(MetricPayload payload);

    void flush();

    void close();

    String getName();
}

