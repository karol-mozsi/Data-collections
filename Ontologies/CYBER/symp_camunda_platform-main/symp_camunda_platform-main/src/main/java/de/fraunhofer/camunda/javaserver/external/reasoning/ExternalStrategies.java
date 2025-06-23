package de.fraunhofer.camunda.javaserver.external.reasoning;

import de.fraunhofer.camunda.javaserver.reasoning.JenaRuleEngine;
import de.fraunhofer.camunda.javaserver.reasoning.SWRLRuleProcessor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.search.EntitySearcher;

import com.clarkparsia.owlapiv3.OWL;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

@Slf4j
public class ExternalStrategies {

    public static ExternalReasoningStrategy jenaStrategy() {
        return (inputStreamOntology, localVars, defaultPathToOutFile) -> {
            try {
                JenaRuleEngine jenaReasoner = new JenaRuleEngine(inputStreamOntology, defaultPathToOutFile);
                List<String> rules = new ArrayList<>(localVars.values());
                jenaReasoner.inferAndSaveFromRules(rules);

                return defaultPathToOutFile;
            } catch (FileNotFoundException e) {
                System.out.println("Error File not Found!");
                log.error("Error File not Found!", e);
                throw new RuntimeException(e);
            }
        };
    }

    public static ExternalReasoningStrategy swrlStrategy() {
        return (inputStreamOntology, localVars, defaultPathToOutFile) -> {
            SWRLRuleProcessor swrl = new SWRLRuleProcessor(inputStreamOntology);
            swrl.addRules(localVars);
            swrl.infer();
            try {
                swrl.saveOntology(defaultPathToOutFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            return defaultPathToOutFile;
        };
    }

    public static ExternalReasoningStrategy serviceMergerStrategy() {
    	final String DELIMITER = "#";
    	final String BASE_ONTO_IRI = "http://iosb.fraunhofer.de/ICS-Security";
        return (inputStreamOntology, localVars, defaultPathToOutFile) -> {
        	/**
        	 * Sets the owl:sameAs axiom for equal services. Equal services are individuals of type  BASE_ONTO_IRI + DELIMITER + "Service"
        	 * which have equal OWLLiterals for the data properties BASE_ONTO_IRI + DELIMITER + "hasName", BASE_ONTO_IRI + DELIMITER + "port"
        	 * and BASE_ONTO_IRI + DELIMITER + "layer4Protocol".
        	 */
    		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    		OWLOntology baseOnt = null;
    		
    		try {
    			baseOnt = m.loadOntologyFromOntologyDocument(inputStreamOntology);
    		} catch (OWLOntologyCreationException e) {
                log.error("Error loading ontology document!", e);
                throw new RuntimeException();
    		}
    		final Set<OWLClassAssertionAxiom> axioms = baseOnt.getClassAssertionAxioms(OWL.Class(IRI.create(BASE_ONTO_IRI + DELIMITER + "Service")));
    		ArrayList<OWLIndividual> sameAsList = new ArrayList<>();
    		OWLDataFactory dataFactory = m.getOWLDataFactory();
    		
    		for(OWLClassAssertionAxiom axiom : axioms) {
    			OWLIndividual individual = axiom.getIndividual();
    			Multimap<OWLDataPropertyExpression, OWLLiteral> dataProperties = EntitySearcher.getDataPropertyValues(individual, baseOnt);
    			OWLLiteral hasNameIndividual = Iterables.get(dataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "hasName")), 0);
    			OWLLiteral portIndividual = Iterables.get(dataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "port")), 0);
    			OWLLiteral layer4ProtocolIndividual = Iterables.get(dataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "layer4Protocol")), 0);
    			
    			boolean found = false;
    			
    			for(OWLIndividual individualInList : sameAsList) {
    				Multimap<OWLDataPropertyExpression, OWLLiteral> tmpDataProperties = EntitySearcher.getDataPropertyValues(individualInList, baseOnt);
    				
    				if( Iterables.get(tmpDataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "hasName")), 0).equals(hasNameIndividual) 
    						&& Iterables.get(tmpDataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "port")), 0).equals(portIndividual)
    						&& Iterables.get(tmpDataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "layer4Protocol")), 0).equals(layer4ProtocolIndividual)) {
    					found = true;
    					Set<OWLIndividual> individuals = new HashSet<>();
    					individuals.add(individual);
    					individuals.add(individualInList);
    					m.addAxiom(baseOnt, dataFactory.getOWLSameIndividualAxiom(individuals));
    				}
    			}
    			
    			if(!found) {
    				sameAsList.add(individual);
    			}
    		}
    		
    		FileOutputStream outputStream;
    		try {
    			outputStream = new FileOutputStream(defaultPathToOutFile);
    		} catch (FileNotFoundException e) {
    			log.error("File not found!", e);
    			throw new RuntimeException();
    		}
    		OWLDocumentFormat serialization = new RDFXMLDocumentFormat();
    		try {
    			m.saveOntology(baseOnt, serialization, outputStream);
    		} catch (OWLOntologyStorageException e) {
    			e.printStackTrace();
    		}
    		
    		return defaultPathToOutFile;
        };
    }
}
