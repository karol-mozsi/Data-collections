package de.fraunhofer.camunda.javaserver.external;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

@FunctionalInterface
public interface ExternalExecutor {
    /**
     * This method should be responsible for executing the main logic of this external worker.
     * The state of other methods while not in scope of this methods is undefined,
     * since initialization to the current externalTask was not completed yet.
     * Override this method to define your own logic. The stub method does nothing by default.
     * <br><br>
     * Other methods may rely on {@code externalTask}. {@code externalTask} can be thought of the data structure that provides
     * context to the currently fetched job.
     *
     * @param externalTask        The current external unit of work fetched by this worker
     * @param externalTaskService Snapshot of the task service with which the user
     *                            may perform extended operations for this execution.
     */
    void executeBusinessLogic(ExternalTask externalTask,
                              ExternalTaskService externalTaskService);
}
