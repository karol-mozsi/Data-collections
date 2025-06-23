package de.fraunhofer.camunda.webapp.reasoning;

import de.fraunhofer.camunda.javaserver.reasoning.JenaRuleEngine;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;

/**
 * @author Florian Patzer florian.patzer@iosb.fraunhofer.de Test for the basic actions concerning
 * Jena's general rules engine
 */
public class JenaRulesTest extends OWLTest {

    private Model model;

    @Before
    public void setupOntExpectedModel() {
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.read(new File("src/test/resources", "reasoning.owl").getAbsolutePath(), "RDF/XML");
    }

    @Test
    public void test() throws IOException {
        HashMap<String, String> rules = new HashMap<>();
        rules.put("testRule1",
                "[testrule: (?d1 rdf:type ICS-Security:HardwareDevice),(?d2 rdf:type ICS-Security:HardwareDevice),notEqual(?d1, ?d2)->(?d1 ICS-Security:notRedundant ?d2)]");

        synchronized (JenaRulesTest.ONTOLOGY_FILE) {
            JenaRuleEngine nrb = new JenaRuleEngine(new FileInputStream(JenaRulesTest.ONTOLOGY_FILE), JenaRulesTest.ONTOLOGY_FILE);
            nrb.addRules(rules);
            nrb.infer();
            nrb.saveOntology();

            // Test if contents of test.owl and test_expected.owl are equal after inferring new ontology
            assertTrue(nrb.getInfModel()
                          .getDeductionsModel()
                          .difference(model)
                          .isEmpty());
        }
    }

    @Test
    public void testAddJohnSmith() throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("src/test/resources", "testAddJohnSmith.owl"));
        JenaRuleEngine jena = new JenaRuleEngine(is, new File("src/test/resources", "testAddJohnSmith2.owl"));
        jena.inferAndSaveFromRules(Collections.singletonList("[testrule: (?d1 rdf:type ICS-Security:HardwareDevice),(?d2 rdf:type ICS-Security:HardwareDevice),notEqual(?d1, ?d2)->(?d1 ICS-Security:notRedundant ?d2)]"));
    }


}
