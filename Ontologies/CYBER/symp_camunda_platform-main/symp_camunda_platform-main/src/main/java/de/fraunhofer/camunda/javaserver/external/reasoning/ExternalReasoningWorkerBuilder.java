package de.fraunhofer.camunda.javaserver.external.reasoning;

import java.io.File;

public class ExternalReasoningWorkerBuilder implements ExternalReasoningWorker.ReasoningWorkerDirBuilder, ExternalReasoningWorker.ReasoningWorkerStrategyBuilder, ExternalReasoningWorker.ReasoningWorkerIdBuilder, ExternalReasoningWorker.ReasoningWorkerTopicBuilder, ExternalReasoningWorker.ReasoningWorkerOptionalBuilder {

    private File outputDir;
    private String id;
    private String topic;
    private String[] variables;
    private ExternalReasoningStrategy externalReasoningStrategy;
    private String endpoint;

    private ExternalReasoningWorkerBuilder() {
    }

    /**
     *
     * @return A new instance to this builder. Builders may be reused by repeatedly calling {@link ExternalReasoningWorker.ReasoningWorkerOptionalBuilder#build()}. Not that the same Strategy Object should not be reused (without synchronization) if it causes side effects, since workers are executed concurrently!
     */
    public static ExternalReasoningWorker.ReasoningWorkerDirBuilder getInstance() {
        return new ExternalReasoningWorkerBuilder();
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerIdBuilder setOutputDirectory(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerOptionalBuilder setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerOptionalBuilder setVariables(String... vars) {
        this.variables = vars;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerOptionalBuilder setReasoningStrategyOpt(ExternalReasoningStrategy reasoningStrategy) {
        this.externalReasoningStrategy = reasoningStrategy;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerOptionalBuilder setWorkerIdOpt(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerOptionalBuilder setOutputDirectoryOpt(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerOptionalBuilder setRestEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override
    public ExternalReasoningWorker build() {
        return endpoint != null ? new ExternalReasoningWorker(id,outputDir, topic,externalReasoningStrategy, endpoint, variables) : new ExternalReasoningWorker(id, outputDir, topic, externalReasoningStrategy, variables);
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerStrategyBuilder setWorkerId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ExternalReasoningWorker.ReasoningWorkerTopicBuilder setReasoningStrategy(ExternalReasoningStrategy reasoningStrategy) {
        this.externalReasoningStrategy = reasoningStrategy;
        return this;
    }
}
