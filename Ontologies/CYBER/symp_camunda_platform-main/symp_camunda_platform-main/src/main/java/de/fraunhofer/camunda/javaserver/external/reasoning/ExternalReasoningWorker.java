package de.fraunhofer.camunda.javaserver.external.reasoning;

import de.fraunhofer.camunda.javaserver.external.ExternalWorker;
import de.fraunhofer.camunda.javaserver.utils.BPMNXmlParser;
import de.fraunhofer.camunda.javaserver.utils.FtpClient;
import lombok.Setter;

import org.camunda.bpm.ProcessEngineService;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.RuntimeServiceImpl;
import org.camunda.bpm.engine.impl.scripting.engine.VariableScopeResolver;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.spin.SpinList;
import org.camunda.spin.json.SpinJsonNode;

import static org.camunda.spin.Spin.JSON;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.management.RuntimeErrorException;

/**
 * A class that serves as a wrapper (facade pattern) for interacting with a running camunda process engine.
 * On construction, this worker creates a new {@link Thread} in the currently running process.
 */
@Setter
public class ExternalReasoningWorker extends ExternalWorker {


    private ExternalReasoningStrategy reasoningStrategy;

    /**
     * A class that serves as a wrapper (facade pattern) for interacting with a running Camunda process engine.
     * On construction, this worker creates a new {@link Thread} in the currently running process.
     *
     * @param id                The unique ID of this worker
     * @param outputDir         Default output Directory that is forwarded to the respective {@code reasoningStrategy}
     * @param topic             The topic defines the unique type of work this worker might accept. Topics can be set in the Camunda Modeler
     * @param reasoningStrategy Strategy that is executed when this worker receives a unit of work from the backend
     * @param variables         These variables declare which values may get fetched from the Processing Engine. If no variables are given then all variables are fetched by default.
     */
    public ExternalReasoningWorker(String id, File outputDir, String topic, ExternalReasoningStrategy reasoningStrategy, String... variables) {
        super(id, outputDir, topic, variables);
        this.reasoningStrategy = reasoningStrategy;
    }

    /**
     * A class that serves as a wrapper (facade pattern) for interacting with a running camunda process engine.
     * On construction, this worker creates a new {@link Thread} in the currently running process.
     *
     * @param id                The unique ID of this worker
     * @param outputDir         Default output Directory that is forwarded to the respective {@code reasoningStrategy}
     * @param topic             The topic defines the unique type of work this worker might accept. Topics can be set in the Camunda Modeler
     * @param reasoningStrategy Strategy that is executed when this worker receives a unit of work from the backend
     * @param variables         These variables declare which values may get fetched from the Processing Engine. If no variables are given then all variables are fetched by default.
     */
    public ExternalReasoningWorker(String id, File outputDir, String topic, ExternalReasoningStrategy reasoningStrategy, String endpoint, String... variables) {
        super(id, outputDir, topic, endpoint, variables);
        this.reasoningStrategy = reasoningStrategy;
    }



    /**
     * This method tries to retrieve the ontology file that is associated with the previous ontology in the process definition.
     * If no such ontology exists an exception is thrown.
     *
     * @return If there is an ontology associated with the previous task retrieve it, otherwise throw Exception
     * @throws IOException 
     */
    protected InputStream getParentOntologyFileAsStream() throws IOException {
    	String currentOntologies = (String) ProcessEngines.getDefaultProcessEngine().getRuntimeService().getVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies");
    	if(currentOntologies == null) {
	        FileValue oldOntFile;
	        // [ONTMERGE] -> [GATEWAY] -> [Jena Task] ...  In this case the ontmerge Id should be returned since this was the last task that saved an ontology and not gateway.
	        if (this.getBpmnXmlParser()
	                .wasForked(this.currentExternalTask.getActivityId())) {
	            oldOntFile = this.currentExternalTask.getVariableTyped(this.getBpmnXmlParser()
	                                                                       .getForkedSourceNode(this.currentExternalTask.getActivityId())
	                                                                       .getId());
	        } else {
	            String parentTaskActivityId = this.getBpmnXmlParser()
	                                     .getSourceFlowNode(this.currentExternalTask.getActivityId())
	                                     .getId();
	            oldOntFile = this.currentExternalTask.getVariableTyped(parentTaskActivityId);
	        }
	        if(oldOntFile == null) {
	            throw new RuntimeException(new IllegalAccessException("Could not find previous ontology file. Please correct your process definition!"));
	        }
	        return oldOntFile.getValue();
    	} else {
    		String predActivityId = null;
    		String forked = "";
    		if (this.getBpmnXmlParser().wasForked(this.currentExternalTask.getActivityId())) {
    			predActivityId = this.getBpmnXmlParser().getForkedSourceNode(this.currentExternalTask.getActivityId()).getId();
    			forked = "forked";
    		} else {
    			predActivityId = this.getBpmnXmlParser().getSourceFlowNode(this.currentExternalTask.getActivityId()).getId();
    		}
    		System.out.println("Task " + this.currentExternalTask.getActivityId() + " has " + forked + " source node " + predActivityId);
    		SpinJsonNode jsonCurrentOntologies = JSON(currentOntologies);
    		if(!jsonCurrentOntologies.isArray()) {
    			throw new RuntimeException("Variable current_ontologies found but is not an array");
    		} else {
    			SpinList<SpinJsonNode> currentOntologiesList = jsonCurrentOntologies.elements();

				System.out.println(currentOntologiesList.toString() + " : " + predActivityId);
    			for(SpinJsonNode n : currentOntologiesList) {
    				System.out.println("comparing " + n.prop("activityId").stringValue() + " with " + predActivityId);
    				if(n.prop("activityId").stringValue().equals(predActivityId)){		    					
		    			String filePath = n.prop("path").stringValue().replaceAll("\"", ""); 
		    			if(filePath.startsWith("ftp:")) {
		    				System.out.println(filePath + " starts with ftp:");
		    				int folderIndex = filePath.lastIndexOf("/");
		    				if(!FtpClient.download("/tmp/" + filePath.substring(folderIndex + 1), filePath)) {
		    					throw new RuntimeException("Unable to download file " + filePath);
		    				} else {
		    					return new FileInputStream("/tmp/" + filePath.substring(folderIndex + 1));
		    				}
		    			} else {
		    				System.out.println(n.prop("path").stringValue() + " does not start with ftp:");
			    			try {
								return new FileInputStream(filePath);
							} catch (FileNotFoundException e) {
								throw new RuntimeException("File " + filePath + " not found!");
							}
		    			}
    				}
    			}

				throw new RuntimeException("File for activity Id " + predActivityId + " not found!");
    			
    		}
    	}
    }


    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        this.currentExternalTask = externalTask;
        this.setCurrentExternalTaskService(externalTaskService);
        this.setBpmnXmlParser(new BPMNXmlParser(this.currentExternalTask.getProcessDefinitionId()));

        File defaultPath = new File(getOntFilePath(currentExternalTask.getActivityId()));

        try {
            File target = reasoningStrategy.executeReasoningLogic(getParentOntologyFileAsStream(), getLocalStringMap(), defaultPath);
            completeTaskWithFile(target, externalTaskService, externalTask);
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter printWriter = new PrintWriter(sw);
            e.printStackTrace(printWriter);
            printWriter.flush();
            externalTaskService.handleFailure(externalTask, "Exception while executing strategy, Error Code is " + e.getMessage(), sw.toString(), 0, 0);
        }

        this.currentExternalTask = null;
        this.setCurrentExternalTaskService(null);
        this.setBpmnXmlParser(null);
    }


    private void completeTaskWithoutFile() {
        if (this.getCurrentExternalTaskService() == null || this.getCurrentExternalTask() == null) {
            throw new RuntimeException(new IllegalAccessException("Cannot complete task from method that isn't associated with the execution of a certain task!"));
        }
        this.getCurrentExternalTaskService()
            .complete(this.getCurrentExternalTask());
    }

    public void logTaskVariables() {
        Map<String, Object> allVars = currentExternalTask.getAllVariables();
        log("all variables in task : " + allVars);
        log("task local strings : " + this.getLocalStrings());
        log("task local rule map : " + this.getLocalStringMap());
    }


    public interface ReasoningWorkerDirBuilder {
        /**
         * Sets the default output dir on this worker. See {@link ExternalWorker#setDefaultDir(File)}, for more information on the use of this default directory.
         *
         * @param outputDir The default directory that the strategy receives on invocation.
         * @return The Reasoning Worker Builder for IDs
         */
        ReasoningWorkerIdBuilder setOutputDirectory(File outputDir);
    }

    public interface ReasoningWorkerIdBuilder {
        /**
         * Set the specific ID for this worker. See {@link ExternalWorker#setId(String)} for more information on the purpose of IDs in Workers.
         *
         * @param id ID for worker
         * @return The Reasoning Worker Builder for Strategies
         */
        ReasoningWorkerStrategyBuilder setWorkerId(String id);
    }

    public interface ReasoningWorkerStrategyBuilder {
        /**
         * Set the Reasoning Strategy on this worker. See {@link ExternalReasoningStrategy#executeReasoningLogic(InputStream, Map, File)} for more information.
         *
         * @param reasoningStrategy Set {@link ExternalReasoningStrategy} on this specific Worker
         * @return Ontology Worker Topic Builder that is next in chained builder calls.
         */
        ReasoningWorkerTopicBuilder setReasoningStrategy(ExternalReasoningStrategy reasoningStrategy);
    }

    public interface ReasoningWorkerTopicBuilder {
        /**
         * Set current topic on this worker. For more information on topics reference {@link ExternalReasoningWorker#ExternalReasoningWorker(String, File, String, ExternalReasoningStrategy, String...)}.
         *
         * @param topic Name of the associated topic for this worker.
         * @return Optional Worker Builder to set all other options that are not necessarily required for valid execution.
         */
        ReasoningWorkerOptionalBuilder setTopic(String topic);
    }

    public interface ReasoningWorkerOptionalBuilder {
        /**
         *
         * The set of variables that are fetched by this worker on task invocation. If not variables are given, all process variables in scope are fetched by default.
         * See {@link ExternalReasoningWorker#ExternalReasoningWorker(String, File, String, ExternalReasoningStrategy, String...)} for more information on variables.
         *
         * @param vars Variables of this worker
         * @return Another Builder to continue chained method calls
         */
        ReasoningWorkerOptionalBuilder setVariables(String... vars);
        /**
         * Set the Reasoning Strategy on this worker. See {@link ExternalReasoningStrategy#executeReasoningLogic(InputStream, Map, File)} for more information.
         *
         * @param reasoningStrategy Set {@link ExternalReasoningStrategy} on this specific Worker
         * @return Another Builder to continue chained method calls
         */
        ReasoningWorkerOptionalBuilder setReasoningStrategyOpt(ExternalReasoningStrategy reasoningStrategy);
        /**
         * Set the specific ID for this worker. See {@link ExternalWorker#setId(String)} for more information on the purpose of IDs in Workers.
         *
         * @param id ID for worker
         * @return Another Builder to continue chained method calls
         */
        ReasoningWorkerOptionalBuilder setWorkerIdOpt(String id);
        /**
         * Sets the default output dir on this worker. See {@link ExternalWorker#setDefaultDir(File)}, for more information on the use of this default directory.
         *
         * @param outputDir The default directory that the strategy receives on invocation.
         * @return Another Builder to continue chained method calls
         */
        ReasoningWorkerOptionalBuilder setOutputDirectoryOpt(File outputDir);

        /**
         * Sets the Camunda REST endpoint for this service worker. See {@link ExternalWorker#ExternalWorker(String, File, String, String, String...)} for more information regarding this endpoint.
         * Possible default values are mentioned in {@link ExternalWorker#ExternalWorker(String, File, String, String...)}.
         *
         * @param endpoint This endpoint should point to the Camunda REST interface
         * @return Another Builder to continue chained method calls
         */
        ReasoningWorkerOptionalBuilder setRestEndpoint(String endpoint);

        /**
         * Finalize the build process for this {@link ExternalReasoningWorker}. This method basically should just call the constructor for a new ExternalReasoningWorker object and return it.
         * But implementation depends on the concrete Builder class.
         *
         * @return An ExternalReasoning Worker. Note that the object itself does not provide many functionality to the user. Instead most logic should come from {@link ExternalReasoningStrategy#executeReasoningLogic(InputStream, Map, File)}
         */
        ExternalReasoningWorker build();
    }

}
