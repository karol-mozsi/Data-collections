package de.fraunhofer.camunda.javaserver.external;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.io.File;
import java.time.LocalTime;

@Slf4j
public class LoggerTestWorker extends ExternalWorker implements ExternalExecutor{


    /**
     * A class that serves as a wrapper (facade pattern) for interacting with a running camunda process engine.
     * On construction, this worker creates a new {@link Thread} in the currently running process.
     *
     * @param id        This id needs to be globally unique across all workers working on the same Process Engine, since the ID specifies which worker may unlock or lock a task.
     * @param outputDir The directory to which resulting ontologies may be written.
     * @param variables Specify variables to avoid fetching everything and only pull in what this task needs for execution.
     */
    public LoggerTestWorker(String id, File outputDir, String... variables) {
        super(id, outputDir, ExternalWorkerTopics.LOGGERTEST.toString(), variables);
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {

        log("Logger Test Worker with ID " + externalTask.getWorkerId() + " working");
        log("\n\n  ... LoggerDelegate invoked by "
                + "processDefinitionId=" + externalTask.getProcessDefinitionId()
                + ", activityId=" + externalTask.getActivityId()
                + " \n\n");
        log("Executing business logic for worker with id {}", this.getId());

        for (int i = 0; i < 1000; i++) {
            log(externalTask.getWorkerId() + " " + externalTask.getActivityId() + " " + i);
        }

        log("Logger Test Worker with ID " + externalTask.getWorkerId() + " finished sleeping ... completing");
        externalTaskService.complete(externalTask);
        log("Logger Test Worker with ID " + externalTask.getWorkerId() + " completed at {}", LocalTime.now());
    }
}
