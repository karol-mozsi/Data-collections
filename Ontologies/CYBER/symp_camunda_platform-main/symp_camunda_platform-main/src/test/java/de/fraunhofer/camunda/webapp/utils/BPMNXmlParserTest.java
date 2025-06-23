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

public class BPMNXmlParserTest {

    private String bpmnXMLString;
    private BpmnModelInstance modelInstance;
    private BPMNXmlParser xmlParser;

    @Before
    public void setup() {
        String resourcesPath = "src/test/resources/BPMN20RestResponse.bpmn";
        File file = new File(resourcesPath);
        modelInstance = Bpmn.readModelFromFile(file);
        xmlParser = new BPMNXmlParser(file);
    }

    @Test
    public void findElementById() {
        ServiceTask ontUpload = (ServiceTask) modelInstance.getModelElementById("ServiceTask_0oqyjw3");
        assertEquals("external", ontUpload.getCamundaType());
    }

    @Test
    public void findElementById_BPMNXmlParser() {
        ServiceTask ontUpload = (ServiceTask) xmlParser.getModelElementById("ServiceTask_0oqyjw3");
        assertEquals("external", ontUpload.getCamundaType());
    }

    @Test
    public void getSourceFlowNodeTest() {
        FlowNode incoming = xmlParser.getSourceFlowNode("ExecutionPath11");
        assertEquals("ExclusiveGateway_0c1zmux", incoming.getId());
    }

    @Test
    public void getSourceFlowNodeTest_edgeCase1() {
        assertNull(xmlParser.getSourceFlowNode("StartEvent_1"));
    }

    @Test(expected = RuntimeException.class)
    public void getSourceFlowNodeTest_edgeCase2() {
        xmlParser.getSourceFlowNode("ExclusiveGateway_1n86aq9");
    }

    @Test
    public void wasForkedTest() {
        assertTrue(xmlParser.wasForked("ExecutionPath11"));
    }

    @Test
    public void wasForkedTest_2() {
        assertFalse(xmlParser.wasForked("ServiceTask_1y9sr6x"));
    }

    @Test
    public void wasJoinedTest() {
        assertTrue(xmlParser.wasJoined("ServiceTask_0o0hzs2"));
        assertTrue(xmlParser.wasJoined("ServiceTask_0l4wtja"));
    }

    @Test
    public void wasJoinedTest_2() {
        assertFalse(xmlParser.wasJoined("ExecutionPath11"));
    }

    @Test
    public void getForkedSourceFlowNode() {
        assertEquals("ServiceTask_1y9sr6x", xmlParser.getForkedSourceNode("ExecutionPath11")
                                                     .getId());
        assertEquals("ServiceTask_1y9sr6x", xmlParser.getForkedSourceNode("ExecutionPath21")
                                                     .getId());
        assertEquals("ServiceTask_1y9sr6x", xmlParser.getForkedSourceNode("ExecutionPath41")
                                                     .getId());
    }

    @Test(expected = RuntimeException.class)
    public void getForkedSourceFlowNode_false() {
        assertNotEquals("ServiceTask_1y9sr6x", xmlParser.getForkedSourceNode("ExecutionPath12"));
    }

    @Test
    public void getJoinableOntologies() {
        System.out.println(xmlParser.getJoinableOnts("ServiceTask_0l4wtja"));
        assertThat(xmlParser.getJoinableOnts("ServiceTask_0l4wtja"), containsInAnyOrder(
                Matchers.equalTo("ExecutionPath21"),
                Matchers.equalTo("ExecutionPath41")
        ));
    }

    @Test
    public void getJoinableOntologies_2() {
        System.out.println(xmlParser.getJoinableOnts("ServiceTask_0o0hzs2"));
        assertThat(xmlParser.getJoinableOnts("ServiceTask_0o0hzs2"), containsInAnyOrder(
                Matchers.equalTo("ExecutionPath12"),
                Matchers.equalTo("ServiceTask_1o02y9e")
        ));
    }

    @Test
    public void getParentTaskTopicTest() {
        System.out.println(xmlParser.parentTaskTopic("ServiceTask_1y9sr6x"));
        assertEquals(xmlParser.parentTaskTopic("ServiceTask_1y9sr6x"), ExternalWorkerTopics.ONTUPLOAD);
    }

    @Test
    public void getParentTaskTopicTest_2() {
        System.out.println(xmlParser.parentTaskTopic("ExecutionPath12"));
        assertEquals(xmlParser.parentTaskTopic("ExecutionPath12"), ExternalWorkerTopics.JENA);
    }

    @Test
    public void getParentTaskTopicTest_3() {
        System.out.println(xmlParser.parentTaskTopic("ServiceTask_0l4wtja"));
        assertEquals(xmlParser.parentTaskTopic("ServiceTask_0l4wtja"), ExternalWorkerTopics.NOT_A_SERVICETASK);
    }
}
