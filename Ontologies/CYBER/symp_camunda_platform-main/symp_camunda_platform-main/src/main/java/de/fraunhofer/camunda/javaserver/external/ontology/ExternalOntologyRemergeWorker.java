package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.utils.CommonUtils;
import de.fraunhofer.camunda.javaserver.utils.FtpClient;
import de.fraunhofer.camunda.javaserver.utils.OwlUtils;
import lombok.extern.slf4j.Slf4j;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.spin.SpinList;
import org.camunda.spin.json.SpinJsonNode;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import static org.camunda.spin.Spin.JSON;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExternalOntologyRemergeWorker extends ExternalOntologyWorker implements ExternalExecutor {
	private static final int BUFFER_SIZE = 4096;

    public ExternalOntologyRemergeWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTREMERGE.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask,
                                     ExternalTaskService externalTaskService) {

        TypedValue result;
        
        String filePath = null;
        SpinJsonNode jsonCurrentOntologies = null;
        
        Collection<String> activityIds = this.getBpmnXmlParser().getJoinableOnts(this.currentExternalTask.getActivityId());
        List<DeviceConfig> inputs = null;


    	String currentOntologies = (String) ProcessEngines.getDefaultProcessEngine().getRuntimeService().getVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies");
    	if(currentOntologies == null) {
    		inputs = activityIds.stream()
                               .map(activityId -> {
                          return new DeviceConfig(getOntFilePath(activityId), "");
                      }).collect(Collectors.toList());
    	} else {
    		jsonCurrentOntologies = JSON(currentOntologies);
    		if(!jsonCurrentOntologies.isArray()) {
    			throw new RuntimeException("Variable current_ontologies found but is not an array");
    		} else {
    			SpinList<SpinJsonNode> currentOntologiesList = jsonCurrentOntologies.elements();
    			inputs = new ArrayList<DeviceConfig>();
    			System.out.println("Remerger uses source activity ids " + activityIds.toString() + " for current ontologies:");
    			for(int i = 0; i < currentOntologiesList.size() && i < activityIds.size(); i++) {
    				System.out.println(currentOntologiesList.get(currentOntologiesList.size()-1-i).prop("path").stringValue());
    				if(activityIds.contains(currentOntologiesList.get(currentOntologiesList.size()-1-i).prop("activityId").stringValue())) {
	    				filePath = currentOntologiesList.get(currentOntologiesList.size()-1-i).prop("path").stringValue().replaceAll("\"", "");     				
	    				if(filePath.startsWith("ftp:")) {
	    					System.out.println(filePath + " starts with ftp:");
							try {
								int folderIndex = filePath.lastIndexOf("/");
								if(!FtpClient.download("/tmp/" + filePath.substring(folderIndex + 1), filePath)) {
									throw new RuntimeException("Unable to download file " + filePath);
								}
								
					            inputs.add(new DeviceConfig("/tmp/" + filePath.substring(folderIndex + 1), ""));
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		    			} else {
		    				System.out.println(filePath + " does not start with ftp:");
			    			inputs.add(new DeviceConfig(filePath, ""));
		    			}
    				}
    			}
    			
    		}
    	}
    	
    	
        
        result = mergeOntologies(inputs);

        Map<String, Object> resultMap = new HashMap<>();
        
		if(currentOntologies != null) {
			int folderIndex = filePath.lastIndexOf("/");
			String newFilePath = filePath.substring(0, folderIndex + 1) + this.currentExternalTask.getActivityId() + ".owl";
			try {
				if(!FtpClient.upload((InputStream) result.getValue(), newFilePath)) {
					throw new RuntimeException("Unable to upload file " + newFilePath);
				}
			} catch(IOException e) { 
				e.printStackTrace();
			}

			jsonCurrentOntologies = JSON((String) ProcessEngines.getDefaultProcessEngine().getRuntimeService().getVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies"));
			System.out.println("Trying to add " + newFilePath + " to ontology variable " + jsonCurrentOntologies.toString() + " in " + this.currentExternalTask.getActivityId());
			jsonCurrentOntologies = CommonUtils.addToCurrentOntologies(jsonCurrentOntologies, this.currentExternalTask.getActivityId(), newFilePath);
			ProcessEngines.getDefaultProcessEngine().getRuntimeService().setVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies", jsonCurrentOntologies.toString());
		}
				
        externalTaskService.complete(externalTask);
    }
    

	private TypedValue mergeOntologies(Collection<DeviceConfig> inputOntologies) {
		System.out.println("Merging following ontologies: " + inputOntologies.toString());
		Iterator<DeviceConfig> it = inputOntologies.iterator();
		
		DeviceConfig firstConf = it.next();
		File ontInFile1 = new File(firstConf.getPath());
		final OWLOntologyManager m1 = OWLManager.createOWLOntologyManager();
		OWLOntology ont1 = null;
		
		try {
			ont1 = m1.loadOntologyFromOntologyDocument(ontInFile1);
		} catch (OWLOntologyCreationException e) {
			log.error("Unable to load ontology!", e);
			throw new RuntimeException();
		}
		
		while(it.hasNext()) {
			DeviceConfig c = it.next();
			File tmpOntFile = new File(c.getPath());
			
			final OWLOntologyManager tmpM = OWLManager.createOWLOntologyManager();
			OWLOntology tmpOnt = null;
			
			try {
				tmpOnt = tmpM.loadOntologyFromOntologyDocument(tmpOntFile);
			} catch (OWLOntologyCreationException e) {
				log.error("Unable to load ontology!", e);
				throw new RuntimeException();
			}
			
			final Set<OWLAxiom> axioms = tmpOnt.getABoxAxioms(Imports.INCLUDED);
			for(OWLAxiom a : axioms) {
				if(!ont1.containsAxiom(a)) {
					m1.addAxiom(ont1, a);
				}
			}
		}

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(m1, ont1, combinedOntFileName);
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }

    public static class DeviceConfig {

        private String inputPath;
        private String iriSuffix;

        public DeviceConfig(String inputPath, String iriSuffix) {
            this.inputPath = inputPath;
            this.iriSuffix = iriSuffix;
        }

        public String getIriSuffix() {
            return iriSuffix;
        }

        public String getPath() {
            return this.inputPath;
        }
    }
}
