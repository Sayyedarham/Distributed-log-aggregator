package com.logaggregator.gateway.pipeline;

public class PipelineException extends Exception {

    private final String stageName;

    public PipelineException(String stageName, String message) {
        super("[" + stageName + "] " + message);
        this.stageName = stageName;
    }

    public PipelineException(String stageName, String message, Throwable cause) {
        super("[" + stageName + "] " + message, cause);
        this.stageName = stageName;
    }

    public String getStageName() {
        return stageName;
    }
}

