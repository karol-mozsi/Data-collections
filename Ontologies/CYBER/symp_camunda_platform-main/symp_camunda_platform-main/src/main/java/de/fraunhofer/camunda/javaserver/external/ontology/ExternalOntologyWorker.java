package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.exceptions.DiagramStateException;
import de.fraunhofer.camunda.javaserver.external.ExternalWorker;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

import java.io.File;
import java.util.List;

public abstract class ExternalOntologyWorker extends ExternalWorker {

    public ExternalOntologyWorker(String id, File outputDir, String topic, String... variables) {
        super(id, outputDir, topic, variables);
    }

    protected List<String> fetchOntologyListFromParentUpload(ExternalTask externalTask) throws DiagramStateException {
        TypedValue objectValue = this.currentExternalTask.getVariableTyped(this.getBpmnXmlParser()
                                                                               .getSourceFlowNode(this.currentExternalTask.getActivityId())
                                                                               .getId());

        if (objectValue == null) {
            throw new DiagramStateException("Parent Task of " + externalTask.getActivityId() + " does not produce a result!");
        }

        return (List<String>) objectValue.getValue();
    }
    
    /**
     * This method tries to retrieve the ontology file that is associated with the previous ontology in the process definition.
     * If no such ontology exists an exception is thrown.
     *
     * @return If there is an ontology associated with the previous task retrieve it, otherwise throw Exception
     */
    protected String getParentOntologyFileName() {
        FileValue oldOntFile;
        // [ONTMERGE] -> [GATEWAY] -> [Jena Task] ...  In this case the ontmerge Id should be returned since this was the last task that saved an ontology and not gateway.
        if (this.getBpmnXmlParser()
                .wasForked(this.currentExternalTask.getActivityId())) {
            oldOntFile = this.currentExternalTask.getVariableTyped(this.getBpmnXmlParser()
                                                                       .getForkedSourceNode(this.currentExternalTask.getActivityId())
                                                                       .getId());
        } else {
            String parentOntId = this.getBpmnXmlParser()
                                     .getSourceFlowNode(this.currentExternalTask.getActivityId())
                                     .getId();
            oldOntFile = this.currentExternalTask.getVariableTyped(parentOntId);
        }
        if(oldOntFile == null) {
            throw new RuntimeException(new IllegalAccessException("Could not find previous ontology file. Please correct your process definition!"));
        }
        return oldOntFile.getFilename();
    }
}
