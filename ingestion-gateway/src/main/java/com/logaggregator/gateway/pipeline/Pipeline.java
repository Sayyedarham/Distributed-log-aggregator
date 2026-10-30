package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.model.MetricPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pipeline {

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    private final List<PipelineStage> stages = new ArrayList<>();

    public Pipeline addStage(PipelineStage stage) {
        if (stage != null) {
            stages.add(stage);
        }
        return this;
    }

    public List<PipelineStage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    public MetricPayload execute(MetricPayload input) throws PipelineException {
        MetricPayload current = input;
        for (PipelineStage stage : stages) {
            if (stage.shouldProcess(current)) {
                try {
                    current = stage.process(current);
                    if (current == null) {
                        logger.debug("Stage {} dropped payload", stage.getName());
                        return null;
                    }
                } catch (PipelineException e) {
                    logger.error("Pipeline failure in stage {}: {}", stage.getName(), e.getMessage());
                    throw e;
                } catch (Exception e) {
                    logger.error("Unexpected error in stage {}: {}", stage.getName(), e.getMessage(), e);
                    throw new PipelineException(stage.getName(), "Unexpected failure: " + e.getMessage(), e);
                }
            }
        }
        return current;
    }
}

