package de.fraunhofer.camunda.javaserver.external.ontology.securityplatform;

import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.parser.SWRLParseException;


public class PhaseARulesCreator {
    private final SWRLRuleEngine baseEngine;

    public PhaseARulesCreator(OWLHelper baseHelper) {
        baseEngine = baseHelper.getSWRLEngine();
    }

    public void createRules() {
        String rule = getMarkRedundanciesRule();

        try {
            baseEngine.createSWRLRule("markRedundancies", rule);
        } catch (SWRLParseException | SWRLBuiltInException ex) {
            System.err.println("Error creating SWRL Rule: " + ex.getMessage() + "\nRule is:\n" + rule.replace('^', '\n'));
        }
    }

    private String getMarkRedundanciesRule() {
        String rule = "Network(?n1) ^ Network(?n2) ^ ipV4Address(?n1, ?addrN1)"
                + " ^ ipV4Address(?n2, ?addrN2) ^ swrlb:equal(?addrN1, ?addrN2)"
                + " ^ prefixBits(?n1, ?bitsN1) ^ prefixBits(?n2, ?bitsN2)"
                + " ^ swrlb:equal(?bitsN1, ?bitsN2) -> utils:redundantTo(?n1, ?n2)";

        return rule;
    }
}
