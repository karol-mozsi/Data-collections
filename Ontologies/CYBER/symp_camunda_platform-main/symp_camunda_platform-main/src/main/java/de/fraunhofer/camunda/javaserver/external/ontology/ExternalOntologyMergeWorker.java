package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyRemergeWorker.DeviceConfig;
import de.fraunhofer.camunda.javaserver.utils.CommonUtils;
import de.fraunhofer.camunda.javaserver.utils.FtpClient;
import de.fraunhofer.camunda.javaserver.utils.OwlUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.spin.SpinList;
import org.camunda.spin.json.SpinJsonNode;
import org.jetbrains.annotations.NotNull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import static org.camunda.spin.Spin.JSON;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExternalOntologyMergeWorker extends ExternalOntologyWorker implements ExternalExecutor {
	private static final int BUFFER_SIZE = 4096;
    private final String DELIMITER = "#";

    public ExternalOntologyMergeWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTMERGE.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask,
                                     ExternalTaskService externalTaskService) {

        TypedValue result;
        Map<String, Object> resultMap = new HashMap<>();
        String currentOntologies = (String) ProcessEngines.getDefaultProcessEngine().getRuntimeService().getVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies");
        
    	if(currentOntologies == null) {
	        if (this.getBpmnXmlParser()
	                .parentTaskTopic(this.currentExternalTask.getActivityId())
	                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
	            Collection<String> ontologyURIList = fetchOntologyListFromParentUpload(externalTask);
	            log("Ontology List: {}", ontologyURIList);
	            try {
	                result = mergeOntologiesFromPaths(ontologyURIList);
	            } catch (IllegalAccessException e) {
	                throw new RuntimeException(e);
	            }
	        } else {
	            List<DeviceConfig> inputs = this.getBpmnXmlParser()
	                                           .getJoinableOnts(this.currentExternalTask.getActivityId())
	                                           .stream()
	                                           .map(activityId -> {
	                                      FileValue val = currentExternalTask.getVariableTyped(activityId);
	                                      return new DeviceConfig(val.getFilename(), extractIriSuffix(val.getFilename()));
	                                  })
	                                           .collect(Collectors.toList());
	            result = mergeOntologiesFromConfig(inputs);
	        }
	        resultMap.put(externalTask.getActivityId(), result);
    	} else {
            SpinJsonNode jsonCurrentOntologies = JSON(currentOntologies);
    		if(!jsonCurrentOntologies.isArray()) {
    			throw new RuntimeException("Variable current_ontologies found but is not an array");
    		} else {
    			String predActivityId = this.getBpmnXmlParser().getSourceFlowNode(this.currentExternalTask.getActivityId()).getId();
    			if (this.getBpmnXmlParser().parentTaskTopic(this.currentExternalTask.getActivityId())
    	                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
    				throw new RuntimeException("Merge task after non-ONTUPLOAD not allowed!");
    			}
    			SpinList<SpinJsonNode> currentOntologiesList = jsonCurrentOntologies.elements();
    			List<DeviceConfig> inputs = new ArrayList<DeviceConfig>();
    			String filePath = null;
    			for(int i=0; i<currentOntologiesList.size(); i++) {
    				if(currentOntologiesList.get(currentOntologiesList.size()-1-i).prop("activityId").stringValue().equals(predActivityId)) {
	    				filePath = currentOntologiesList.get(currentOntologiesList.size()-1-i).toString().replaceAll("\"", "");     				
	    				if(filePath.startsWith("ftp:")) {
	    					System.out.println(filePath + " starts with ftp:");
	    					URL url;
							try {
		    					int folderIndex = filePath.lastIndexOf("/");
		    					if(!FtpClient.download("/tmp/" + filePath.substring(folderIndex + 1), filePath)) {
		    						throw new RuntimeException("Unable to download file " + filePath);
		    					}
					            inputs.add(new DeviceConfig("/tmp/" + filePath.substring(folderIndex + 1), extractIriSuffix(filePath.substring(folderIndex + 1))));
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		    			} else {
		    				System.out.println(currentOntologiesList.get(0).toString() + " does not start with ftp:");
			    			throw new RuntimeException("Entries of current_ontologies variable can only use schema ftp: ");
		    			}
    				}
    			}
    			if(inputs.isEmpty()) {
    				throw new RuntimeException("No file path with ONTUPLOAD task " + predActivityId + " found.");
    			}
    			result = mergeOntologiesFromConfig(inputs);
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
    			jsonCurrentOntologies = CommonUtils.addToCurrentOntologies(jsonCurrentOntologies, this.currentExternalTask.getActivityId(), newFilePath);
    			ProcessEngines.getDefaultProcessEngine().getRuntimeService().setVariable(this.currentExternalTask.getProcessInstanceId(),"current_ontologies", jsonCurrentOntologies.toString());
    		}
    	}

        externalTaskService.complete(externalTask);
    }
    
    /**
     * Extracts the IRI suffix following a naming convention where the file always ends with -irisuffix.owl where irisuffix is the suffix.
     * Note that the irisuffix can neither contain a dash nor a dot.
     * @param filename Filename to extract the suffix from
     * @return The suffix as String
     */
    private String extractIriSuffix(String filename) {
    	log("Received Filename " + filename);
		int dotIndex = filename.lastIndexOf(".");
		int dashIndex = filename.lastIndexOf("-");
		return filename.substring(dashIndex + 1, dotIndex);
	}

	private TypedValue mergeOntologiesFromConfig(Collection<DeviceConfig> inputOntologies) {
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		System.out.println("Merging ontologies: " + inputOntologies.toString());
		Iterator<DeviceConfig> ontIt = inputOntologies.iterator();
		DeviceConfig firstConf = inputOntologies.iterator().next();
		
    	File baseOntInFile = new File(firstConf.getPath());
		OWLOntology baseOnt = null;
		
		try {
			baseOnt = m.loadOntologyFromOntologyDocument(baseOntInFile);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			return null;
		}
		
		final String baseIri = baseOnt.getOntologyID().getOntologyIRI().get().toString();
		OWLEntityRenamer renamer = new OWLEntityRenamer(m, Collections.singleton(baseOnt));
		HashMap<OWLEntity, IRI> entToIri = new HashMap<>();
		
		baseOnt.getIndividualsInSignature().forEach(indiv -> {
			entToIri.put(indiv, IRI.create(indiv.getIRI().toString().replace(baseIri, baseIri + "/" + firstConf.getIriSuffix())));
		});
		
		m.applyChanges(renamer.changeIRI(entToIri));
		
		while(ontIt.hasNext()) {
			DeviceConfig conf = ontIt.next();
			OWLOntology tmpOnt = null;
			final HashMap<OWLEntity, IRI> entityToIri = new HashMap<>();
			final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			try {
				tmpOnt = man.loadOntologyFromOntologyDocument(new File(conf.getPath()));
			} catch (OWLOntologyCreationException e) {
				log.error("Unable to load ontology with path" + conf.getPath());
				e.printStackTrace();
				return null;
			}
			renamer = new OWLEntityRenamer(man, Collections.singleton(tmpOnt));
			
			tmpOnt.getIndividualsInSignature().forEach(indiv -> {
				entityToIri.put(indiv, IRI.create(indiv.getIRI().toString().replace(baseIri, baseIri + "/" + conf.getIriSuffix())));
			});
			
			man.applyChanges(renamer.changeIRI(entityToIri));
			Set<OWLAxiom> tmpAxioms = tmpOnt.getABoxAxioms(null);
			m.addAxioms(baseOnt, tmpAxioms);
		}

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(m, baseOnt, combinedOntFileName);
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }

    private TypedValue mergeOntologiesFromPaths(Collection<String> paths) throws IllegalAccessException {
        List<DeviceConfig> inputOntologies = paths.stream()
                                                  .map(s -> {
                                                      return new DeviceConfig(s, FilenameUtils.getBaseName(s));
                                                  })
                                                  .collect(Collectors.toList());
       return mergeOntologiesFromConfig(inputOntologies);
    }

    @NotNull
    private OntModel extendOntModel(OntModel union, DeviceConfig conf) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.read(conf.getPath(), "RDF/XML");

        // Get Iterator for all "type"-properties with range/type "NamedIndividual"
        Property a = model.getProperty(model.getNsPrefixURI("rdf") + "type");
        Resource namedIndividual = model.getResource(model.getNsPrefixURI("owl") + "NamedIndividual");
        ResIterator rit = model.listResourcesWithProperty(a, namedIndividual);

        // Go through all (named) individuals of the current model
        ArrayList<String> newNamespaces = new ArrayList<>();
        while (rit.hasNext()) {
            Resource individual = rit.next();

            int dIndex = individual.getNameSpace()
                                   .indexOf(DELIMITER);
            String newNamespace =
                    individual.getNameSpace()
                              .substring(0, dIndex) + "/" + conf.getIriSuffix() + DELIMITER;

            if (!newNamespaces.contains(newNamespace)) {
                newNamespaces.add(newNamespace);
                // Add prefix for new namespace and add namespace itself
                String nsUriPrefix = model.getNsURIPrefix(individual.getNameSpace());
                nsUriPrefix = nsUriPrefix == null ? "" : nsUriPrefix;
                model.setNsPrefix(nsUriPrefix + conf.getIriSuffix(), newNamespace);
            }
            ResourceUtils.renameResource(individual, newNamespace + individual.getLocalName());
        }

        return ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, union.union(model));
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
