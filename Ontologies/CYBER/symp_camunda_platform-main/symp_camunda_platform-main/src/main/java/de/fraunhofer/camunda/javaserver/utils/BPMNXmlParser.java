package de.fraunhofer.camunda.javaserver.utils;

import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.rest.RestClient;
import lombok.Getter;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public class BPMNXmlParser {

    private RestClient restClient = new RestClient();
    private String bpmnXML;
    private BpmnModelInstance bpmnModelInstance;

    public BPMNXmlParser(String processDefinitionId) {
        bpmnXML = getBPMNXmlFromID(processDefinitionId);
        bpmnModelInstance = Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnXML.getBytes(StandardCharsets.UTF_8)));
    }

    public BPMNXmlParser(File bpmnFile) {
        bpmnModelInstance = Bpmn.readModelFromFile(bpmnFile);
    }

    public <T extends ModelElementInstance> T getModelElementById(String id) {
        return this.bpmnModelInstance.getModelElementById(id);
    }

    /**
     * This method tries to find the incoming source node of the given element with id. If no such node can be found then null is returned. If multiple nodes are incoming then
     * a runtime exception is thrown.
     *
     * @param id The unique id of the model element to get the source flow node from. Note that the element may only have one incoming sequence flow.
     * @return The parent flow node if only one exists. If no parent exists return null.
     */
    public FlowNode getSourceFlowNode(String id) {
        FlowNode source = this.bpmnModelInstance.getModelElementById(id);
        Collection<SequenceFlow> incoming = source.getIncoming();
        if (incoming.size() > 1) {
            throw new RuntimeException(new Exception("Cannot get Source Flow Node of Element that has multiple incoming SequenceFlows"));
        }
        Iterator<SequenceFlow> it = incoming.iterator();
        return it.hasNext() ? it.next()
                                .getSource() : null;
    }

    public FlowNode getForkedSourceNode(String id) {
        if (!wasForked(id)) {
            throw new RuntimeException(new Exception("Node was not forked!"));
        }
        FlowNode gateway = getSourceFlowNode(id);
        return getSourceFlowNode(gateway.getId());
    }

    public boolean wasForked(String id) {
        FlowNode source = getSourceFlowNode(id);
        if (isGateway(source)) {
            return source.getIncoming()
                         .size() <= 1;
        }
        return false;
    }

    public boolean wasJoined(String id) {
        FlowNode source = getSourceFlowNode(id);
        if (isGateway(source)) {
            return source.getIncoming()
                         .size() > 1;
        }
        return false;
    }

    public ExternalWorkerTopics parentTaskTopic(String activityId) {
        if (!(getSourceFlowNode(activityId) instanceof ServiceTask)) {
            return ExternalWorkerTopics.NOT_A_SERVICETASK;
        }

        return ExternalWorkerTopics.valueOf(((ServiceTask) getSourceFlowNode(activityId)).getCamundaTopic()
                                                                                         .toUpperCase());
    }

    public Collection<String> getJoinableOnts(String activityId) {
        if (!wasJoined(activityId)) {
            throw new RuntimeException(new Exception("Node has to be target of join to get joinable Ontologies!"));
        }

        FlowNode source = getSourceFlowNode(activityId);
        List<FlowNode> sources = source.getIncoming()
                                       .stream()
                                       .map(SequenceFlow::getSource)
                                       .collect(Collectors.toList());

        return sources.stream()
                      .map(BaseElement::getId)
                      .collect(Collectors.toList());
    }


    private boolean isGateway(FlowNode node) {
        ModelElementType type = node.getElementType();
        while (type != null) {
            if (type.getTypeName()
                    .equals("gateway")) {
                return true;
            }
            type = type.getBaseType();
        }
        return false;
    }

    private String getBPMNXmlFromID(String processDefinitionId) {
        return ((LinkedHashMap<String, String>) this.restClient.get("process-definition/" + processDefinitionId + "/xml")).get("bpmn20Xml");
    }
}
