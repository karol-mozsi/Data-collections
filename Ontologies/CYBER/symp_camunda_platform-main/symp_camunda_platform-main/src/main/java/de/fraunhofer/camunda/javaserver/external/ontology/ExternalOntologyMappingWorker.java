package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyRemergeWorker.DeviceConfig;
import de.fraunhofer.camunda.javaserver.rest.TypedVariable;
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
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.AutoIRIMapper;

import static org.camunda.spin.Spin.JSON;

import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ExternalOntologyMappingWorker extends ExternalOntologyWorker implements ExternalExecutor {
	private static final int BUFFER_SIZE = 4096;

    public ExternalOntologyMappingWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTMAPPING.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask,
                                     ExternalTaskService externalTaskService) {

        TypedValue result;
        Map<String, Object> resultMap = new HashMap<>();

        String filePath = null;
        Collection<String> inputOntologyList = null;
        List<DeviceConfig> inputs = null;
        
        SpinJsonNode jsonCurrentOntologies = null;
        String currentOntologies = (String) ProcessEngines.getDefaultProcessEngine().getRuntimeService().getVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies");

        Map<String, String> prefixFtpPathMapping = getLocalStringMap();
        
        List<String> ftpPaths = new ArrayList<>();                
        List<String> localPaths = new ArrayList<>();
        
        //-------------------Load ontology from current ontology variable----------------
    	if(currentOntologies == null) {
	        if (this.getBpmnXmlParser()
	                .parentTaskTopic(this.currentExternalTask.getActivityId())
	                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
	            inputOntologyList = fetchOntologyListFromParentUpload(externalTask);
	            log("Ontology List: {}", inputOntologyList);
	        } else {
	        	throw new RuntimeException("Ontology-Mapping is not allowed to follow any other task than ONTUPLOAD");
	        }
    	} else {
    		inputOntologyList = new ArrayList<String>();
    		jsonCurrentOntologies = JSON(currentOntologies);
    		if(!jsonCurrentOntologies.isArray()) {
    			throw new RuntimeException("Variable current_ontologies found but is not an array");
    		} else {
    			String predActivityId = null;
        		if (this.getBpmnXmlParser().wasForked(this.currentExternalTask.getActivityId())) {
        			 predActivityId = this.getBpmnXmlParser().getForkedSourceNode(this.currentExternalTask.getActivityId()).getId();
        		} else {
        			predActivityId = this.getBpmnXmlParser().getSourceFlowNode(this.currentExternalTask.getActivityId()).getId();
        		}
    			SpinList<SpinJsonNode> currentOntologiesList = jsonCurrentOntologies.elements();
				for(SpinJsonNode n : currentOntologiesList) {
    				if(n.prop("activityId").stringValue().equals(predActivityId)){
						filePath = n.prop("path").stringValue().replaceAll("\"", ""); 
						String localFile = downloadFromFtp(filePath, "/tmp/");
						if(localFile == null) {
							throw new RuntimeException("Unable to download file " + filePath);
						}
						inputOntologyList.add(localFile);
    				}
    			}
    		}
    	}
    	//--------------------------------
    	
    	//-----------------Load ontologies from task variables-------------------------    	
    	//Remember one ftp path
    	String ftpPath = prefixFtpPathMapping.values().iterator().next();
    	prefixFtpPathMapping.forEach((String prefix, String p)->{
    		if(p.startsWith("ftp:")) {
    			prefixFtpPathMapping.replace(prefix, p, downloadFromFtp(p, this.getOutputDir().getAbsolutePath()));
    		} else {
    			File f = new File(p);
    			if (f.isDirectory()) {
    				throw new RuntimeException("File expected bud folder given: " + p);
    			} else {
    				prefixFtpPathMapping.replace(prefix, p, f.getAbsolutePath());
    			}
    		}
    	});		
	
    	System.out.println("Mapping input Ontology List: " +inputOntologyList.toString());
		result = mergeOntologies(inputOntologyList, prefixFtpPathMapping);

		
        if(currentOntologies != null) {
			int folderIndex = filePath.lastIndexOf("/");
			String newFilePath = filePath.substring(0, folderIndex + 1) + this.currentExternalTask.getActivityId() + ".owl";
			try {
				System.out.println("Trying to upload file " + newFilePath);
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
    
    public static String downloadFromFtp(String ftpUri, String outputDir) {
    	if(ftpUri.startsWith("ftp:")) {
    		System.out.println(ftpUri + " starts with ftp:");
    	} else {
    		throw new RuntimeException("Only FTP URIs accepted, but got following path:" + ftpUri);
    	}

		try {
			int folderIndex = ftpUri.lastIndexOf("/");
			String newFilePath = outputDir + ftpUri.substring(folderIndex + 1);
			
			if(!FtpClient.download(newFilePath, ftpUri)) {
				throw new RuntimeException("Unable to download file " + ftpUri);
			} else {
				return newFilePath;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;		
    }
   
    private TypedValue mergeOntologies(Collection<String> inputOntologyList, Map<String, String> ontologiesToIntegrade) {
		AutoIRIMapper mapper = new AutoIRIMapper(new File(this.getOutputDir().getAbsolutePath()), false);
		mapper.update();
		
		Iterator<String> inputIterator = inputOntologyList.iterator();
		AtomicReference<OWLOntologyManager> targetM = new AtomicReference<OWLOntologyManager>(OWLManager.createOWLOntologyManager());
		File targetOntInFile = new File(inputIterator.next());
		AtomicReference<OWLOntology> targetOnt = new AtomicReference<>();		
		try {
			targetOnt.set(targetM.get().loadOntologyFromOntologyDocument(targetOntInFile));
		} catch (OWLOntologyCreationException e) {
			log.error("Unable to load ontology!", e);
			throw new RuntimeException();
		}
		targetM.get().getIRIMappers().add(mapper);
		AtomicReference<OWLDocumentFormat> formatTarget = new AtomicReference<OWLDocumentFormat>(targetM.get().getOntologyFormat(targetOnt.get()));
		
		ontologiesToIntegrade.forEach((String prefix, String filePath)->{
			OWLOntology mappingOnt = null;
			
			File ontToIntegrateFile = new File(filePath);
			
			final OWLOntologyManager ontToIntegrateManager = OWLManager.createOWLOntologyManager();
			
			try {
				mappingOnt = ontToIntegrateManager.loadOntologyFromOntologyDocument(ontToIntegrateFile);
			} catch (OWLOntologyCreationException e) {
				log.error("Unable to load ontology!", e);
				throw new RuntimeException();
			}
			System.out.println("Trying to set " + prefix + ":" +  mappingOnt.getOntologyID().getOntologyIRI().get().toString());
			if(prefix.contains("_string")) {
				prefix = prefix.replace("_string", "");
			}
			formatTarget.get().asPrefixOWLOntologyFormat().setPrefix(prefix, mappingOnt.getOntologyID().getOntologyIRI().get().toString() + "#");
						
			final Set<OWLAxiom> axioms = mappingOnt.getAxioms(Imports.INCLUDED);
			targetM.get().addAxioms(targetOnt.get(), axioms);
		});
		targetM.get().setOntologyFormat(targetOnt.get(), formatTarget.get());
		formatTarget.get().asPrefixOWLOntologyFormat().getPrefixName2PrefixMap().forEach((String k, String v)->{
			System.out.println("Prefix " + k + " maps to " + v);
		});

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(targetM.get(), targetOnt.get(), combinedOntFileName, formatTarget.get());
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }
}
