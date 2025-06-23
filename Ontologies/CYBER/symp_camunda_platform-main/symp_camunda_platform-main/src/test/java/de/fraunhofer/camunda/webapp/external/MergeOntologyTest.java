package de.fraunhofer.camunda.webapp.external;

import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyMergeWorker;
import org.apache.commons.io.FilenameUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MergeOntologyTest {

    private List<String> paths;

    @Before
    public void setup() {
        paths = new ArrayList<String>(Arrays.asList("C:\\Users\\Einstein\\Desktop\\Git\\reasoningbpm\\src\\test\\resources\\testExpansion\\a.txt", "C:\\Users\\Einstein\\Desktop\\Git\\reasoningbpm\\src\\test\\resources\\testExpansion\\b.txt"));
    }

    @Test
    public void testInputOntGeneration() {
        List<ExternalOntologyMergeWorker.DeviceConfig> inputOntologies = paths.stream()
                                                                              .map(s -> {
                                                                            	  return new ExternalOntologyMergeWorker.DeviceConfig(s, FilenameUtils.getBaseName(s));
                                                                              })
                                                                              .collect(Collectors.toList());
        assertEquals(2, inputOntologies.size());
        assertThat(inputOntologies, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasProperty("path"),
                        Matchers.hasProperty("iriSuffix", Matchers.equalTo("a"))
                ),
                Matchers.allOf(
                        Matchers.hasProperty("path"),
                        Matchers.hasProperty("iriSuffix", Matchers.equalTo("b"))
                )
        ));
    }
}
