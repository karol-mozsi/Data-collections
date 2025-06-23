package de.fraunhofer.camunda.javaserver.external.ontology;

import com.clarkparsia.owlapiv3.OWL;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntologyException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class MergeUtilOWL {

    private final static String DELIMITER = "#";
    private final static String BASE_ONTO_IRI = "http://iosb.fraunhofer.de/ICS-Security";

    /**
     * Sets the owl:sameAs axiom for equal services. Equal services are individuals of type  BASE_ONTO_IRI + DELIMITER + "Service"
     * which have equal OWLLiterals for the data properties BASE_ONTO_IRI + DELIMITER + "hasName", BASE_ONTO_IRI + DELIMITER + "port"
     * and BASE_ONTO_IRI + DELIMITER + "layer4Protocol".
     */
    public static void mergeServices(File input, File output) throws IOException, OWLOntologyStorageException {

        final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntology baseOnt = null;

        try {
            baseOnt = m.loadOntologyFromOntologyDocument(input);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return;
        }
        final Set<OWLClassAssertionAxiom> axioms = baseOnt.getClassAssertionAxioms(OWL.Class(IRI.create(BASE_ONTO_IRI + DELIMITER + "Service")));
        ArrayList<OWLIndividual> sameAsList = new ArrayList<>();
        OWLDataFactory dataFactory = m.getOWLDataFactory();

        for (OWLClassAssertionAxiom axiom : axioms) {
            OWLIndividual individual = axiom.getIndividual();
            Multimap<OWLDataPropertyExpression, OWLLiteral> dataProperties = EntitySearcher.getDataPropertyValues(individual, baseOnt);
            OWLLiteral hasNameIndividual = Iterables.get(dataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "hasName")), 0);
            OWLLiteral portIndividual = Iterables.get(dataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "port")), 0);
            OWLLiteral layer4ProtocolIndividual = Iterables.get(dataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "layer4Protocol")), 0);

            boolean found = false;

            for (OWLIndividual individualInList : sameAsList) {
                Multimap<OWLDataPropertyExpression, OWLLiteral> tmpDataProperties = EntitySearcher.getDataPropertyValues(individualInList, baseOnt);

                if (Iterables.get(tmpDataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "hasName")), 0)
                             .equals(hasNameIndividual)
                        && Iterables.get(tmpDataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "port")), 0)
                                    .equals(portIndividual)
                        && Iterables.get(tmpDataProperties.get(OWL.DataProperty(BASE_ONTO_IRI + DELIMITER + "layer4Protocol")), 0)
                                    .equals(layer4ProtocolIndividual)) {
                    found = true;
                    Set<OWLIndividual> individuals = new HashSet<>();
                    individuals.add(individual);
                    individuals.add(individualInList);
                    m.addAxiom(baseOnt, dataFactory.getOWLSameIndividualAxiom(individuals));
                }
            }

            if (!found) {
                sameAsList.add(individual);
            }
        }

        saveOntology(m, baseOnt, output);
    }

    public static void mergeOwl(List<File> file, String output) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
        mergeOwl(file.stream()
                     .map(File::getAbsolutePath)
                     .collect(Collectors.toList()), new File(output));
    }


    /**
     * Merges the RDF/XML encoded ontologies given by the paths in {@code inputOntologies} to the base ontology using OWLAPI.
     * Each ongology axiom of the ABox gets a suffix in order to prevent naming issues.
     *
     * @param paths  List of file paths to .owl resources. The IRI suffix of each respective new ontology is inferred from the files basename.
     * @param output Destination of this merge operation on the file system.
     */
    public static void mergeOwl(List<String> paths, File output) throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
        List<DeviceConfig> inputOntologies = paths.stream()
                                                  .map(s -> new DeviceConfig(s, FilenameUtils.getBaseName(s)))
                                                  .collect(Collectors.toList());

        final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        if (inputOntologies.isEmpty()) {
            throw new OntologyException("Could not merge list of empty ontologies! There needs to exist at least one base ontology from which can be merged from.");
        }
        OWLOntology baseOnt = m.loadOntologyFromOntologyDocument(new File(inputOntologies.remove(0).path));

        System.out.println(baseOnt.getOntologyID()
                                  .getOntologyIRI()
                                  .get());

        final String baseIri = baseOnt.getOntologyID()
                                      .getOntologyIRI()
                                      .get()
                                      .toString();

        for (DeviceConfig conf : inputOntologies) {
            OWLOntology tmpOnt;
            HashMap<OWLEntity, IRI> entityToIri = new HashMap<>();
            OWLOntologyManager man = OWLManager.createOWLOntologyManager();
            tmpOnt = man.loadOntologyFromOntologyDocument(new File(conf.getPath()));
            OWLEntityRenamer renamer = new OWLEntityRenamer(man, Collections.singleton(tmpOnt));

            tmpOnt.getIndividualsInSignature()
                  .forEach(indiv -> {
                      entityToIri.put(indiv, IRI.create(indiv.getIRI()
                                                             .toString()
                                                             .replace(baseIri, baseIri + "/" + conf.getIriSuffix())));
                  });

            man.applyChanges(renamer.changeIRI(entityToIri));

            Set<OWLAxiom> tmpAxioms = tmpOnt.getABoxAxioms(null);
            m.addAxioms(baseOnt, tmpAxioms);
        }
        saveOntology(m, baseOnt, output);
    }

    private static void saveOntology(OWLOntologyManager manager, OWLOntology ontology, File output) throws OWLOntologyStorageException, IOException {
        try (FileOutputStream outputStream = new FileOutputStream(output)) {
            OWLDocumentFormat serialization = new RDFXMLDocumentFormat();
            manager.saveOntology(ontology, serialization, outputStream);
        }
    }

    /**
     * Container representing needed data for a device ontology. Contains an iri suffix and the file path of the ontology.
     *
     */
    @Getter
    @Setter
    @AllArgsConstructor
    private static class DeviceConfig {
        private String path;
        private String iriSuffix;
    }
}
