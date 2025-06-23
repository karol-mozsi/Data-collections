package de.fraunhofer.camunda.webapp.external;


import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class ExternalStrategiesTest {

    @Autowired
    public ProcessEngineRule processEngineRule;

    @Test
    @Ignore
    @Deployment
    public void externalTaskServiceTest() {
        RuntimeService runtimeService = processEngineRule.getRuntimeService();
        ExternalTaskService externalTaskService = processEngineRule.getExternalTaskService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ruleEngineExternal");

        ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();


        assertNotNull(externalTask.getId());
        assertEquals("ServiceTask_0kyn4ww", externalTask.getActivityId());
        assertNotNull(externalTask.getActivityInstanceId());
        assertNotNull(externalTask.getExecutionId());
        assertEquals(processInstance.getProcessDefinitionId(), externalTask.getProcessDefinitionId());
        assertEquals("oneExternalTaskProcess", externalTask.getProcessDefinitionKey());
        assertEquals("jena", externalTask.getTopicName());
        assertNull(externalTask.getWorkerId());
        assertNull(externalTask.getLockExpirationTime());
        assertFalse(externalTask.isSuspended());

    }

}
