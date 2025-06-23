package de.fraunhofer.camunda.webapp.external;

import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyUploadWorker;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class ExternalOntologyUploadWorkerTest {

    private List<String> filePaths;

    @Before
    public void before() {
        filePaths = Arrays.asList("src/test/resources/testExpansion", "src/test/resources/app.properties");
    }

}
