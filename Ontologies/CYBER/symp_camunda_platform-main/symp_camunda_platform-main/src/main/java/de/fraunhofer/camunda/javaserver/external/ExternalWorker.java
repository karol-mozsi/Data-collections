package de.fraunhofer.camunda.javaserver.external;

import de.fraunhofer.camunda.javaserver.exceptions.HTTPException;
import de.fraunhofer.camunda.javaserver.rest.RestClient;
import de.fraunhofer.camunda.javaserver.rest.TypedVariable;
import de.fraunhofer.camunda.javaserver.utils.BPMNXmlParser;
import de.fraunhofer.camunda.javaserver.utils.CommonUtils;
import de.fraunhofer.camunda.javaserver.utils.FtpClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.spin.json.SpinJsonNode;

import static org.camunda.spin.Spin.JSON;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class ExternalWorker implements ExternalTaskHandler {
	private static final int BUFFER_SIZE = 4096;

    public static final String DEFAULT_ENDPOINT = "http://localhost:8080/engine-rest";
    /**
     * This {@code File} should point to the directory where ontologies
     * are persisted after execution of business logic. This attribute overrides the {@link #defaultDirectory}.
     *
     * @see #getOntFilePath(String)
     * @see #setDefaultDir(File)
     */
    public File outputDir;
    /**
     * The current context for this unit of work. <br> <br>
     * <p>
     * {@code currentExternalTask} might be used to fetch activity or process definition id's.
     */
    public ExternalTask currentExternalTask;
    private ExternalTaskService currentExternalTaskService;
    private static File defaultDirectory;
    private static final String replacementPattern = "\\{}";
    private static volatile AtomicInteger globalId = new AtomicInteger(0);
    private ExternalTaskClient externalTaskClient;
    private RestClient restClient = new RestClient();
    private BPMNXmlParser bpmnXmlParser;
    private String topic;
    /**
     * The ID for workers should be unique by design. Internally the ID is responsible for mapping locks to specific jobs to workers.
     *
     * @param id Id that is set on this worker. Note that this ID has to be unique by design.
     * @return id Returns the ID currently associated with this worker.
     */
    private String id;
    private ExternalExecutor externalExecutor;

    /**
     * The default directory may be set for all workers once instead of specifying a directory for each individual worker.
     * Note that {@link #outputDir} overrides the default directory for all workers.
     *
     * @param dir The directory that is set as default.
     */
    public static void setDefaultDir(File dir) {
        defaultDirectory = dir;
    }

    /**
     * A class that serves as a wrapper (facade pattern) for interacting with a running camunda process engine.
     * On construction, this worker creates a new {@link java.lang.Thread} in the currently running process. Note that if you want to add custom logic to this base class without
     * extending it you can use the {@link de.fraunhofer.camunda.javaserver.external.ExternalWorkerBuilder} or set {@link #externalExecutor}.
     *
     * @param id        This id needs to be globally unique across all workers working on the same Process Engine, since the ID specifies which worker may unlock or lock a task.
     * @param outputDir The directory to which resulting ontologies may be written.
     * @param topic     The topic to which this worker should listen to.
     * @param endpoint  The REST endpoint where this worker should point to.
     * @param variables Specify variables to avoid fetching everything and only pull in what this task needs for execution.
     */
    protected ExternalWorker(String id, File outputDir, String topic, String endpoint, String... variables) {
        this.topic = topic;
        this.id = id;
        this.externalExecutor = (a, s) -> {
        };
        this.setOutputDir(outputDir);
        this.externalTaskClient = ExternalTaskClient.create()
                                                    .baseUrl(endpoint)
                                                    .workerId(id)
                                                    .asyncResponseTimeout(10000)
                                                    .maxTasks(1)
                                                    .build();
        subscribe(topic, variables);
        log.info("Worker with id {} and topic {} created. Timeout set to 30000ms", globalId.get(), topic.toString()
                                                                                                        .toLowerCase());
    }

    private void subscribe(String topic, String[] variables) {
        TopicSubscriptionBuilder builder = this.getExternalTaskClient()
                                               .subscribe(topic
                                                       .toLowerCase())
                                               .lockDuration(10000)
                                               .handler(this);

        if (variables != null && variables.length != 0) {
            builder = builder.variables(variables);
        }

        builder.open();
    }

    /**
     * A class that serves as a wrapper (facade pattern) for interacting with a running camunda process engine.
     * On construction, this worker creates a new {@link java.lang.Thread} in the currently running process. Note that if you want to add custom logic to this base class without
     * extending it you can use the {@link de.fraunhofer.camunda.javaserver.external.ExternalWorkerBuilder} or set {@link #externalExecutor}.
     *
     * Using this constructor the default {@code REST Endpoint} from {@link ExternalWorker#ExternalWorker(String, File, String, String, String...)} will be set to {@value DEFAULT_ENDPOINT}
     *
     * @param id        This id needs to be globally unique across all workers working on the same Process Engine, since the ID specifies which worker may unlock or lock a task.
     * @param outputDir The directory to which resulting ontologies may be written.
     * @param topic     The topic to which this worker should listen to.
     * @param variables Specify variables to avoid fetching everything and only pull in what this task needs for execution.
     */
    protected ExternalWorker(String id, File outputDir, String topic, String... variables) {
        this(id, outputDir, topic, DEFAULT_ENDPOINT, variables);
    }


    /**
     * This helper method may be used to manually verify the location to which this worker persists its ontology on completion.
     * For a given activityName this method builds an absolute path in the current filesytem.
     * <br><br>
     * Note that this method may not be called without specifying <i>any</i> directory first.
     * You can register your directory by calling {@link #setOutputDir(File)} for a dir local to this worker or
     * {@link #setDefaultDir(File)} for a global directory definition.
     *
     * @param activityName The name for which the current output file path should be fetched.
     * @return An absolute path as string pointing to the resource in the
     * file system where this worker may persist its output ontology to.
     * @see #setOutputDir(File)
     * @see #setDefaultDir(File)
     * @since 1.0.0
     */
    public String getOntFilePath(String activityName) {
        File ontFilePath = defaultDirectory == null ? outputDir : defaultDirectory;
        return ontFilePath.getAbsolutePath() + "_" + activityName + ".owl";
    }


    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        this.currentExternalTask = externalTask;
        this.currentExternalTaskService = externalTaskService;
        this.bpmnXmlParser = new BPMNXmlParser(this.currentExternalTask.getProcessDefinitionId());
        externalExecutor.executeBusinessLogic(externalTask, externalTaskService);
        this.currentExternalTask = null;
        this.bpmnXmlParser = null;
        this.currentExternalTaskService = null;
    }

    /**
     * Prints your debug message to console. <br><br>
     * You may call the method like {@code debug("Critical Error {} has occurred!", "42")}
     * to print {@code "Critical Error 42 has occurred!"}.
     *
     * @param msg  Your debug message to print to console
     * @param args Args are dynamically inserted into the debug message as seen above.
     */
    public void debug(String msg, Object... args) {
        System.out.println("#DEBUG " + LocalTime.now() + " [" + topic.toString() + " - " + this.currentExternalTask.getActivityId() + "] " + formatMessage(msg, args));
    }

    /**
     * Prints your log message to console. <br><br>
     * You may call the method like {@code info("Status Code is {}", "42")}
     * to print {@code "#INFO LocalTime.now() [topicName - activityId] Status Code is 42"}.
     *
     * @param msg  Your info message to print to console
     * @param args Args are dynamically inserted into the info message as seen above.
     */
    public void log(String msg, Object... args) {
        System.out.println("#INFO " + LocalTime.now() + " [" + topic.toString() + " - " + this.currentExternalTask.getActivityId() + "] " + formatMessage(msg, args));
    }

    private String formatMessage(String msg, Object... args) {
        String res = msg;
        for (Object a : args) {
            String tmp = a.toString()
                          .replaceAll("\\\\", "\\\\\\\\");
            res = res.replaceFirst(replacementPattern, tmp);
        }
        return res;
    }

    /**
     * This method is similar to {@link #getLocalStringMap()}
     * in that it returns task variables. <br>
     * In comparison to those other two functions this method only returns a simple mapping of the variables string to a typed variable.
     * A typed variable is simply a container for all kinds of variables that could be received from the backend. Note that this method may
     * only be called inside the functional {@link #externalExecutor} method.
     *
     * @return Local Variables to the current external task
     * @throws IllegalAccessException Throws an IllegalAccessException if method called while externalTask not initialized.
     */
    public Map<String, ? extends TypedVariable> getLocalVariables() throws IllegalAccessException {
        if (this.currentExternalTask == null) {
            throw new IllegalAccessException("May only acces this method after currentExternalTask is initialized!");
        }
        return this.restClient.getTaskLocalVariables(this.currentExternalTask);
    }

    /**
     * This method is similar to {@link #getLocalVariables()} and {@link #getLocalStringMap()}
     * in that it returns task variables. However the caveat to those other methods is that
     * a lot of meta information is returned that might not be needed.
     * <br><br>
     * <p>
     * This method simple returns the list of all strings it could find in the current task definition.
     *
     * @return "Normalized" List of Strings this task received on input
     */
    public List<String> getLocalStrings() {
        return this.restClient.getTaskLocalStrings(this.currentExternalTask);
    }

    /**
     * This method is similar to {@link #getLocalVariables()} and {@link #getLocalStringMap()}
     * in that it returns task variables.
     * In contrast to the other two functions, this function is actually relatively smart in that it will try to
     * coerce different types such as text and list into map entries and output a collection of those coerced entries to the user.
     *
     * @return This
     */
    public Map<String, String> getLocalStringMap() {
        return this.restClient.getTaskLocalStringMap(this.currentExternalTask);
    }

    /**
     * Simply deletes a process variable from a given process instance.
     *
     * @param processInstanceId The process name from which the variable should be deleted from
     * @param varName           The name of the variable that is up for removal
     * @throws HTTPException Exception is thrown if the Process Engine returns with an unexpected status code. This exception can only be ignored if removal of the variable is unnecessary.
     */
    public void deleteProcessVariable(String processInstanceId, String varName) throws HTTPException {
        this.restClient.deleteProcessVariable(processInstanceId, varName);
    }

    /**
     * Completes given tasks while saving the file under the ID of the current activity.
     *
     * @param file                File object to complete this task with
     * @param externalTaskService Task Service that gets invoked to complete task
     * @param externalTask        The task to complete
     */
    public void completeTaskWithFile(File file, ExternalTaskService externalTaskService, ExternalTask externalTask) {
    	Map<String, Object> variables = new HashMap<>();
        String currentOntologies = (String) this.currentExternalTask.getVariable("current_ontologies");
    	if(currentOntologies != null) {
    		SpinJsonNode jsonCurrentOntologies = JSON(currentOntologies);
    		if(!jsonCurrentOntologies.isArray()) {
    			throw new RuntimeException("Variable current_ontologies found but is not an array");
    		} else {
    			String lastFilePath = jsonCurrentOntologies.elements().get(jsonCurrentOntologies.elements().size()-1).prop("path").stringValue();
    			lastFilePath = lastFilePath.replaceAll("\"", "");
    			System.out.println("Using last file path " + lastFilePath);
    			int folderIndex = lastFilePath.lastIndexOf("/");
    			String newFilePath = lastFilePath.substring(0, folderIndex + 1) + this.currentExternalTask.getActivityId() + ".owl";
    			try {
	    			if(!FtpClient.upload(new FileInputStream(file), newFilePath)) {
	    				throw new RuntimeException("Unable to upload file " + newFilePath);
	    			}
    			} catch(IOException e) {
    				e.printStackTrace();
    			}

    			jsonCurrentOntologies = JSON((String) ProcessEngines.getDefaultProcessEngine().getRuntimeService().getVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies"));
    			System.out.println("Trying to add " + newFilePath + " to ontology variable " + jsonCurrentOntologies.toString() + " in " + this.currentExternalTask.getActivityId());
    			jsonCurrentOntologies = CommonUtils.addToCurrentOntologies(jsonCurrentOntologies, this.currentExternalTask.getActivityId(), newFilePath);
    			ProcessEngines.getDefaultProcessEngine().getRuntimeService().setVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies", jsonCurrentOntologies.toString());

    			System.out.println(jsonCurrentOntologies.toString());
    		}
    	} else {
    		throw new RuntimeException("No current ontologies variable found!");
    	}
        externalTaskService.complete(externalTask);//, variables);
    }

    /**
     * This method provides a little helping functionality of simply serializing all variables available to this task.
     *
     * @return String representation of all task variables
     */
    public String getAllVariables() {
        Map<String, Object> allVariables = currentExternalTask.getAllVariables();
        return StringUtils.<Object>join(allVariables);
    }

    /**
     * This method simply creates a result map with {@code Object o} under {@code String varName}.
     * Since one might as why such a thing is convenient, the internal serialization type used by Camunda {@link org.camunda.bpm.engine.variable.value.ObjectValue}
     * does not have to be called explicitly every time one wants to persist an object after task completion.
     *
     * @param varName The variable name that is persisted to the database
     * @param o       The object that is persisted under the variable name
     * @return A "ready to use" map that can be sent back to the server for persisting {@code Object o}.
     */
    public Map<String, Object> getResultMapFromObject(String varName, Object o) {
        Map<String, Object> resultMap = new HashMap<>();
        ObjectValue objectValue = Variables.objectValue(o)
                                           .serializationDataFormat("application/json")
                                           .create();
        resultMap.put(varName, objectValue);
        return resultMap;
    }

    private String externalTaskStringFromMsg(String msg) {
        if (currentExternalTask == null) {
            return "[undefined] " + msg;
        }
        return "[" + currentExternalTask.getActivityId() + "] " + msg;
    }

    @Override
    public String toString() {
        return "{" + this.topic + "-" + this.id + "}";
    }

    public interface WorkerDirBuilder<T extends WorkerIdBuilderParent> {
        T setOutputDirectory(File outputDir);
    }

    public interface WorkerIdBuilderParent {
    }

    public interface WorkerIdBuilderNoTopic extends WorkerIdBuilderParent {
        WorkerOptionalBuilder setWorkerId(String id);
    }

    public interface WorkerIdBuilder extends WorkerIdBuilderParent {
        WorkerExecutorBuilder setWorkerId(String id);
    }

    public interface WorkerExecutorBuilder {
        WorkerTopicBuilder setExternalExecutor(ExternalExecutor externalExecutor);
    }

    public interface WorkerTopicBuilder {
        WorkerOptionalBuilder setTopic(String topic);
    }

    public interface WorkerOptionalBuilder {
        WorkerOptionalBuilder setVariables(String... vars);
        WorkerOptionalBuilder setEndpoint(String endpoint);

        ExternalWorker build();
    }


}
