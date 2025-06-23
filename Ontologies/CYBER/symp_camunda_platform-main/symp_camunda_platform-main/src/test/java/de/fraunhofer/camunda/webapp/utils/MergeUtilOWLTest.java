package de.fraunhofer.camunda.webapp.utils;

import de.fraunhofer.camunda.javaserver.external.ontology.MergeUtilOWL;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MergeUtilOWLTest {

    private List<File> owlResourcesIn = Arrays.asList(new File("src/test/resources/rdfICSSecurityNetwork.rdf"),
            new File("src/test/resources/rdfICSSecurityNetworkCustom.rdf"));
    private File out = new File("src/test/resources/mergeTestResult.owl");

    @Test
    public void testMergeOwl_owlResourcesCorrect_mergeCorrect() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
        MergeUtilOWL.mergeOwl(owlResourcesIn, out.getAbsolutePath());
    }
}
