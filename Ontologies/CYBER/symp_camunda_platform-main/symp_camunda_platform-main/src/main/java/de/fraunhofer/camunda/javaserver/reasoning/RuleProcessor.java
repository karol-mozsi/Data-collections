package de.fraunhofer.camunda.javaserver.reasoning;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public interface RuleProcessor <T,S> {

    void loadOntology(File file) throws OWLOntologyCreationException, FileNotFoundException;

    void addRules(Map<T, S> rules);

    void inferAndSaveFromRules(Map<T, S> rules);

    void saveOntology();

    void infer();
}
