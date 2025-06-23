package de.fraunhofer.camunda.webapp.reasoning;

import de.fraunhofer.camunda.javaserver.reasoning.SWRLRuleProcessor;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Florian Patzer florian.patzer@iosb.fraunhofer.de
 */
public class SwrlApiTest extends OWLTest {


    @Test
    public void testBasicRuleInferring() throws FileNotFoundException {
        HashMap<String, String> rules = new HashMap<>();
        rules.put("testRule1", "ICS-Security:HardwareDevice(?d1)^ICS-Security:HardwareDevice(?d2)^differentFrom(?d1, ?d2)->ICS-Security:notRedundant(?d1, ?d2)");

        synchronized (SwrlApiTest.ONTOLOGY_FILE) {
            SWRLRuleProcessor swrl = new SWRLRuleProcessor(SwrlApiTest.ONTOLOGY_FILE);
            swrl.addRules(rules);
            swrl.infer();
            Set<OWLAxiom> s = swrl.getRuleEngine()
                                  .getInferredOWLAxioms();
            System.out.println(s);
            swrl.saveOntology(new File("src/test/resources", "swrlapitest_3.owl"));
        }
    }

}
