package de.fraunhofer.camunda.javaserver.external.ontology.securityplatform;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.factory.SWRLAPIFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OWLHelper {
    private OWLOntologyManager ontologyManager;
    private OWLOntology ontology = null;
    private OWLDataFactory dataFactory;
    private OWLReasoner reasoner;
    private IRI defaultIRI;
    private IRI queryIRI;
    private Map<String, File> mapping = new HashMap<>();
    private String filename;

    private IRI getQueryIRI(String s) {
        return queryIRI.create(queryIRI + "#" + s);
    }

    private IRI getIRI(String s) {
        return defaultIRI.create(defaultIRI + "#" + s);
    }

    public OWLReasoner getReasoner() {
        return reasoner;
    }

    public OWLHelper(String filename, String queryIRI) {
        this.filename = filename;
        this.queryIRI = IRI.create(queryIRI);
    }

    public OWLHelper(String filename) {
        this.filename = filename;
    }

    public void addFileMapping(Map<String, File> mapping) {
        this.mapping = mapping;
    }

    public void createHelper() {
        ontologyManager = OWLManager.createOWLOntologyManager();
        mapping.forEach((i, f) -> {
            ontologyManager.addIRIMapper(new SimpleIRIMapper(IRI.create(i), IRI.create(f)));
        });
        try {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(new File(filename));
        } catch (OWLOntologyCreationException ex) {
            System.err.println("Error creating ontology from file " + filename + " : " + ex.getMessage());
            System.exit(-1);
        }


        defaultIRI = ontology.getOntologyID().getOntologyIRI().get();
        if (queryIRI == null) {

            queryIRI = ontology.getOntologyID().getOntologyIRI().get();
        }

        this.dataFactory = ontologyManager.getOWLDataFactory();
        reasoner = new PelletReasonerFactory().createReasoner(ontology);
    }

    public void addAxiomsFromOntology(String filename, Map<String, File> mapping) {
        OWLOntologyManager sourceOntologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology sourceOntology = null;
        try {
            mapping.forEach((i, f) -> sourceOntologyManager.addIRIMapper(new SimpleIRIMapper(IRI.create(i), IRI.create(f))));
            sourceOntology = sourceOntologyManager.loadOntologyFromOntologyDocument(new File(filename));
        } catch (OWLOntologyCreationException ex) {
            System.err.println("Error creating ontology from file " + filename + " : " + ex.getMessage());
            System.exit(-1);
        }

        ontologyManager.addAxioms(ontology, sourceOntology.getABoxAxioms(Imports.EXCLUDED));
        reasoner.flush();
    }

    public void saveInferred(String filename) {
        InferredOntologyGenerator generator = new InferredOntologyGenerator(reasoner);
        generator.fillOntology(dataFactory, ontology);
        save(filename);
    }

    public void save(String filename) {
        try {
            ontology.saveOntology(new OWLXMLOntologyFormat(), IRI.create(new File(filename)));
        } catch (OWLOntologyStorageException ex) {
            System.err.println("Error storing ontology to file " + filename + " : " + ex.getMessage());
            System.exit(-1);
        }
    }

    public SWRLRuleEngine getSWRLEngine() {
        return SWRLAPIFactory.createSWRLRuleEngine(ontology);
    }

    public Set<OWLLiteral> getDataProperty(OWLNamedIndividual individual, String propertyName) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(getQueryIRI(propertyName));
        return reasoner.getDataPropertyValues(individual, property);
    }

    public void addDataProperty(OWLIndividual individual, String propertyName, int literal) {
        addDataProperty(individual, propertyName, "" + literal, "int");
    }

    public void addDataProperty(OWLIndividual individual, String propertyName, String literal, String datatypeIRI) {
        OWLDataPropertyExpression property = dataFactory.getOWLDataProperty(getQueryIRI(propertyName));
        OWLDataPropertyAssertionAxiom portAxiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual,
                dataFactory.getOWLLiteral("" + literal, dataFactory.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#" + datatypeIRI))));
        ontologyManager.applyChange(new AddAxiom(ontology, portAxiom));
    }

    public void addDataProperty(OWLIndividual individual, String propertyName, String literal) {
        OWLDataPropertyExpression property = dataFactory.getOWLDataProperty(getQueryIRI(propertyName));
        OWLDataPropertyAssertionAxiom portAxiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, literal);
        ontologyManager.applyChange(new AddAxiom(ontology, portAxiom));
    }

    public void addObjectProperty(OWLIndividual individual, String propertyName, OWLIndividual individual2) {
        OWLObjectPropertyExpression property = dataFactory.getOWLObjectProperty(getQueryIRI(propertyName));
        OWLObjectPropertyAssertionAxiom portAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(property, individual, individual2);
        ontologyManager.applyChange(new AddAxiom(ontology, portAxiom));
    }

    public Set<OWLNamedIndividual> getIndividuals(String classname) {
        OWLClass owlClass = dataFactory.getOWLClass(getQueryIRI(classname));
        return reasoner.getInstances(owlClass, true).getFlattened();
    }

    public Set<OWLNamedIndividual> getObjectProperty(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(getQueryIRI(propertyName));
        return reasoner.getObjectPropertyValues(individual, property).getFlattened();
    }

    public boolean isClass(OWLNamedIndividual individual, String classname) {
        OWLClass owlClass = dataFactory.getOWLClass(getQueryIRI(classname));
        return reasoner.getTypes(individual, false).containsEntity(owlClass);
    }

    public Set<OWLNamedIndividual> filterClass(Set<OWLNamedIndividual> individual, String classname) {
        return individual.stream().filter(i -> isClass(i, classname)).collect(Collectors.toSet());
    }

    public OWLNamedIndividual createIndividual(String name, String classname) {
        OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(getIRI(name));
        OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(dataFactory.getOWLClass(getQueryIRI(classname)), individual);
        ontologyManager.applyChange(new AddAxiom(ontology, classAssertion));
        return individual;
    }

    public Set<OWLNamedIndividual> getObjectProperty(OWLNamedIndividual individual, String propertyName, int minCount, int maxCount) {
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(getQueryIRI(propertyName));
        Set<OWLNamedIndividual> result = reasoner.getObjectPropertyValues(individual, property).getFlattened();
        if (result.size() > maxCount) {
            throw new InconsistentOntologyException("Individual " + individual + " has more than " + maxCount + " " + propertyName + " properties");
        } else if (result.size() < minCount) {
            throw new InconsistentOntologyException("Individual " + individual + " has less than " + minCount + " " + propertyName + " properties");
        } else {
            return result;
        }
    }

    public OWLNamedIndividual getSingleObjectProperty(OWLNamedIndividual individual, String propertyName) {
        return getObjectProperty(individual, propertyName, 1, 1).iterator().next();
    }

    public Set<OWLLiteral> getDataProperty(OWLNamedIndividual individual, String propertyName, int minCount, int maxCount) {
        Set<OWLLiteral> result = getDataProperty(individual, propertyName);
        if (result.size() > maxCount) {
            throw new InconsistentOntologyException("Individual " + individual + " has more than " + maxCount + " " + propertyName + " properties");
        } else if (result.size() < minCount) {
            throw new InconsistentOntologyException("Individual " + individual + " has less than " + minCount + " " + propertyName + " properties");
        } else {
            return result;
        }
    }

    public OWLLiteral getSingleDataProperty(OWLNamedIndividual individual, String propertyName) {
        return getDataProperty(individual, propertyName, 1, 1).iterator().next();
    }

    public void addDifferentIndividuals(Set<OWLNamedIndividual> individuals) {
        OWLDifferentIndividualsAxiom differentAxiom = dataFactory.getOWLDifferentIndividualsAxiom(individuals);
        ontologyManager.applyChange(new AddAxiom(ontology, differentAxiom));
    }

    public OWLDataFactory getDataFactory() {
        return dataFactory;
    }

    public OWLOntology getOntology() {
        return ontology;
    }
}
