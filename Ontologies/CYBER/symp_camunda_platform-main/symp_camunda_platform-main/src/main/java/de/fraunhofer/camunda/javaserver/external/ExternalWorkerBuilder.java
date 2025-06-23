package de.fraunhofer.camunda.javaserver.external;

import java.io.File;

public class ExternalWorkerBuilder implements ExternalWorker.WorkerDirBuilder<ExternalWorker.WorkerIdBuilder>, ExternalWorker.WorkerIdBuilder, ExternalWorker.WorkerTopicBuilder, ExternalWorker.WorkerOptionalBuilder, ExternalWorker.WorkerExecutorBuilder {

    private File outputDir;
    private String id;
    private String topic;
    private String[] variables;
    private String endpoint = null;
    private ExternalExecutor externalExecutor;

    private ExternalWorkerBuilder() {
    }

    public static ExternalWorker.WorkerDirBuilder<ExternalWorker.WorkerIdBuilder> getInstance() {
        return new ExternalWorkerBuilder();
    }

    @Override
    public ExternalWorker.WorkerIdBuilder setOutputDirectory(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    public ExternalWorker.WorkerExecutorBuilder setWorkerId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ExternalWorker.WorkerOptionalBuilder setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public ExternalWorker.WorkerOptionalBuilder setVariables(String... vars) {
        this.variables = vars;
        return this;
    }

    @Override
    public ExternalWorker.WorkerOptionalBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }


    @Override
    public ExternalWorker build() {
        return endpoint != null ? new ExternalWorker(id, outputDir, topic, endpoint, variables) : new ExternalWorker(id, outputDir, topic, variables);
    }

    @Override
    public ExternalWorker.WorkerTopicBuilder setExternalExecutor(ExternalExecutor externalExecutor) {
        this.externalExecutor = externalExecutor;
        return this;
    }
}
