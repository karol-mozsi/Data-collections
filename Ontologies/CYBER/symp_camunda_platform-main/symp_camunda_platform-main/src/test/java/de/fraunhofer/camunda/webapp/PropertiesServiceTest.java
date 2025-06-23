package de.fraunhofer.camunda.webapp;

import static org.junit.Assert.assertEquals;

import de.fraunhofer.camunda.javaserver.utils.PropertiesService;
import org.drools.core.command.assertion.AssertEquals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PropertiesServiceTest {

  @Before
  public void before() {

  }

  @After
  public void after() {

  }

  @Test
  public void testGetProperty() {
    String actual = PropertiesService.getProperty("ontologies");
    assertEquals(actual, "ontologyList");
  }

  @Test
  public void testGetOntologyProperty() {
    String expected = "ontologyList";
    String actual = PropertiesService.getOntologyListString();
    assertEquals(expected, actual);
  }

}
