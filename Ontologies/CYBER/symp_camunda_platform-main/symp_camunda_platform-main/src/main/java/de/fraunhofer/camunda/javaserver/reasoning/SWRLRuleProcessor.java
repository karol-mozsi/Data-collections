package de.fraunhofer.camunda.javaserver.reasoning;

import java.io.*;
import java.util.Map;
import lombok.Getter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * Class implementing the basics for SWRL reasoning using Drools engine
 */
@Getter
public class SWRLRuleProcessor implements RuleProcessor<String, String> {

    private SWRLRuleEngine ruleEngine;
    private OWLOntologyManager ontologyManager;
    private OWLOntology ontology;
    private OWLReasoner reasoner;
    private OWLDataFactory dataFactory;

    public SWRLRuleProcessor(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public SWRLRuleProcessor(InputStream in) {
        ontologyManager = OWLManager.createOWLOntologyManager();
        try {
            loadOntology(in);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        ruleEngine = createRuleEngine(this.ontology);
    }

    @Override
    public void loadOntology(File file) throws OWLOntologyCreationException {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(file);
    }

    public void loadOntology(InputStream in) throws OWLOntologyCreationException {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(in);
    }

    private SWRLRuleEngine createRuleEngine(OWLOntology ontology) {
        return SWRLAPIFactory.createSWRLRuleEngine(ontology);
    }

    @Override
    public void addRules(Map<String, String> rules) {
        if (ruleEngine == null) {
            return;
        }
        rules.forEach((String ruleName, String rule) -> {
            try {
            	System.out.println("Creating rule " + rule);
                ruleEngine.createSWRLRule(ruleName, rule);
            } catch (SWRLParseException | SWRLBuiltInException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void infer() {
        //ruleEngine.infer();
    }

    @Override
    public void saveOntology() {
    	sneakInPellet();
        try {        	
            ontologyManager.saveOntology(ontology);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    public void saveOntology(OutputStream outputStream) {
    	sneakInPellet();
        try {
            ontologyManager.saveOntology(ontology, outputStream);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    public void saveOntology(File file) throws FileNotFoundException {
        saveOntology(new FileOutputStream(file));
    }
    
    private void sneakInPellet() {
    	this.dataFactory = ontologyManager.getOWLDataFactory();
        reasoner = new PelletReasonerFactory().createReasoner(ontology);
        InferredOntologyGenerator generator = new InferredOntologyGenerator(reasoner);
        generator.fillOntology(dataFactory, ontology);
    }

    @Override
    public void inferAndSaveFromRules(Map<String, String> rules) {
        addRules(rules);
        infer();
        saveOntology();
    }
}
