package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.utils.CommonUtils;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.spin.json.SpinJsonNode;

import static org.camunda.spin.Spin.JSON;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This worker either puts ftp paths into the current_ontologies list or creates a collection of local paths, which is only useful for local-only runtime, where
 * no network communication is needed and all external workers can process the collection of local paths. The latter is almost never used. 
 * @author Florian Patzer
 *
 */

public class ExternalOntologyUploadWorker extends ExternalOntologyWorker implements ExternalExecutor {
	private static final int BUFFER_SIZE = 4096;
	private SpinJsonNode jsonCurrentOntologies;

    public ExternalOntologyUploadWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTUPLOAD.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log("All Variables in Task {} : {}", externalTask.getActivityId(), getAllVariables());
        log("Task local Variables in Task {} : {}", externalTask.getActivityId(), this.getLocalStringMap());

        if (this.getBpmnXmlParser()
                .parentTaskTopic(this.currentExternalTask.getActivityId())
                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
        	throw new RuntimeException("Only one ONTUPLOAD task allowed. Add paths to single task if you want to upload them or use ONTMAPPING for model integration.");
        }
        
        this.jsonCurrentOntologies = JSON("[]");
        
        Map<String, Object> parameters = externalTask.getAllVariables();
        
        parameters.forEach((String key, Object value) -> {
        	CommonUtils.addToCurrentOntologies(this.jsonCurrentOntologies, this.currentExternalTask.getActivityId()
    				, value.toString());
        });


    	ProcessEngines.getDefaultProcessEngine().getRuntimeService().setVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies",this.jsonCurrentOntologies.toString());
		externalTaskService.complete(externalTask);//, variables);
    }

    public List<String> expandDirs(List<String> filePaths) {
    	
    		
        return filePaths.stream()
                        .flatMap(s -> {
                        	if(s.startsWith("ftp:")) {
            					this.jsonCurrentOntologies.append(s);
            					return null;
        	    			} else {
	                            File f = new File(s);
	                            if (f.isDirectory()) {
	                                return Arrays.stream(f.listFiles())
	                                             .map(File::getAbsolutePath);
	                            } else {
	                                return Stream.of(new File(s).getAbsolutePath());
	                            }
        	    			}
                        })
                        .collect(Collectors.toList());
    }
    

}
