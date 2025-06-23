package de.fraunhofer.camunda.javaserver.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class OwlUtils {
	public static void owlApiSaveOntology(OWLOntologyManager manager, OWLOntology ontology, String path) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(new File(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		OWLDocumentFormat serialization = new RDFXMLDocumentFormat();
		try {
			manager.saveOntology(ontology, serialization, outputStream);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	public static void owlApiSaveOntology(OWLOntologyManager manager, OWLOntology ontology, String path, OWLDocumentFormat formatTarget) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(new File(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		RDFXMLDocumentFormat serialization = new RDFXMLDocumentFormat();
		if(formatTarget.isPrefixOWLOntologyFormat()) {
			serialization.copyPrefixesFrom(formatTarget.asPrefixOWLOntologyFormat());
		} else {
			throw new RuntimeException("Wrong format received for merged ontology");
		}
		
		try {
			manager.saveOntology(ontology, serialization, outputStream);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
}
