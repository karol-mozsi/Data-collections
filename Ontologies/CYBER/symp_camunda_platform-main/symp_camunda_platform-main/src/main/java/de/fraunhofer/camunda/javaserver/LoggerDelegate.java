package de.fraunhofer.camunda.javaserver;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

@Slf4j
public class LoggerDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {

        log.info("\n\n  ... LoggerDelegate invoked by "
                + "processDefinitionId=" + execution.getProcessDefinitionId()
                + ", activityId=" + execution.getCurrentActivityId()
                + " \n\n");

        Thread.sleep(3000);
    }
}

