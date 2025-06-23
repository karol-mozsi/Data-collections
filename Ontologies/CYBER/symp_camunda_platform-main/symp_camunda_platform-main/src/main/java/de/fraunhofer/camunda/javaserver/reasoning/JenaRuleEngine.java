package de.fraunhofer.camunda.javaserver.reasoning;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.reasoner.rulesys.Rule.ParserException;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A class implementing the basic methods for the Jena general purpose rule engine.
 */
@Getter
@Setter
public class JenaRuleEngine implements RuleProcessor<String, String> {
    private OntModel model;
    private Reasoner reasoner;
    private InfModel infModel;
    private String filePath;
    private String ruleString;
    private OutputStream outFileTarget;

    public JenaRuleEngine(InputStream in, File out) throws FileNotFoundException {
        this.loadOntology(in);
        this.outFileTarget = new FileOutputStream(out);
    }

    public void loadOntology(File file) throws FileNotFoundException {
        this.filePath = file.getAbsolutePath();
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.read(filePath, "RDF/XML");
    }

    public void loadOntology(InputStream in) {
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        // TODO: Is the conversion from relative to absolute URI's here important?
        model.read(in, null);
    }

    public void addRules(Map<String, String> rules) {
        this.addRules(rules.values());
    }

    public void addRules(Collection<String> rules) {
        rules = rules.stream()
                     .map(StringEscapeUtils::unescapeHtml4)
                     .collect(Collectors.toList());
        System.out.println("Reasoning over rules: " + rules);

        Map<String, String> ns = model.getNsPrefixMap();
        ruleString = "";
        ns.forEach((String prefix, String iri) -> {
            ruleString += "@prefix " + prefix + ": <" + iri + "> .\n";
        });

        rules.forEach(rule -> {
            try {
                ruleString += rule + "\n";
            } catch (ParserException e) {
                e.printStackTrace();
            }
        });
        Reader ruleStringReader = new StringReader(ruleString);
        BufferedReader bufferedRuleStringReader = new BufferedReader(ruleStringReader);

        reasoner = new GenericRuleReasoner(Rule.parseRules(Rule.rulesParserFromReader(bufferedRuleStringReader)));
    }

    public void infer() {
        infModel = ModelFactory.createInfModel(reasoner, model);
    }

    public InfModel getInfModel() {
        return infModel;
    }

    public void saveOntology() {
        infModel.write(outFileTarget, "RDFXML");
    }

    /**
     * This method writes the inferred ontology to outPath.
     * If no outPath was specifically set this method writes to the file that was given in the constructor.
     *
     * @param rules List of Rules to infer from.
     */
    public void inferAndSaveFromRules(List<String> rules) {
        addRules(rules);
        infer();
        saveOntology();
    }

    public void inferAndSaveFromRules(Map<String, String> rules) {
        addRules(rules.values());
        infer();
        saveOntology();
    }
}
