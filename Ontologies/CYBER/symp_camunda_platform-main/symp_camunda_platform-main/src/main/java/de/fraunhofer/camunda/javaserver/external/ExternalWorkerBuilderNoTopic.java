package de.fraunhofer.camunda.javaserver.external;

import lombok.Getter;

import java.io.File;

@Getter
public abstract class ExternalWorkerBuilderNoTopic implements ExternalWorker.WorkerDirBuilder<ExternalWorker.WorkerIdBuilderNoTopic>, ExternalWorker.WorkerIdBuilderNoTopic, ExternalWorker.WorkerOptionalBuilder {

    private File outputDir;
    private String[] variables;
    private String id;

    public abstract String getTopic();


    @Override
    public abstract ExternalWorker build();

    @Override
    public ExternalWorker.WorkerIdBuilderNoTopic setOutputDirectory(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    public ExternalWorker.WorkerOptionalBuilder setWorkerId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ExternalWorker.WorkerOptionalBuilder setVariables(String... vars) {
        this.variables = vars;
        return this;
    }


}
