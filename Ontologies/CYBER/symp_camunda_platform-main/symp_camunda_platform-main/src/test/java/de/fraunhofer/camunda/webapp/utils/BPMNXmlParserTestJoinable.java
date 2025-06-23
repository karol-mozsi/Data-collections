package de.fraunhofer.camunda.webapp.utils;

import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.utils.BPMNXmlParser;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class BPMNXmlParserTestJoinable {

    private BPMNXmlParser xmlParser;

    @Before
    public void setup() {
        String resourcesPath = "src/test/resources/definitions/workingexamplePlugin.bpmn";
        File file = new File(resourcesPath);
        xmlParser = new BPMNXmlParser(file);
    }

    @Test
    public void getJoinableOntologies() {
        System.out.println(xmlParser.getJoinableOnts("ServiceTask_16nukyc"));
        assertThat(xmlParser.getJoinableOnts("ServiceTask_16nukyc"), containsInAnyOrder(
                Matchers.equalTo("ServiceTask_1cxhar7"),
                Matchers.equalTo("ServiceTask_0v4cb2h")
        ));
    }


}
