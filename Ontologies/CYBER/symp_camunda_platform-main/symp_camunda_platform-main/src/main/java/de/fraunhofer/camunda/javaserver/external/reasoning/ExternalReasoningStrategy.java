package de.fraunhofer.camunda.javaserver.external.reasoning;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * Reasoning strategies provide the highest level of abstraction from the Process Engine since they are only concerned
 * about the Input and Output Processing of Ontologies.
 */
public interface ExternalReasoningStrategy {

    /**
     * This method is used for ontology manipulation and is expected to provide a valid output file containing data of this operation.
     * The file path does not matter since data is persisted in the Process Engine and not in the File System. Do not reuse the same strategy object for multiple workers without synchronization if the strategy causes
     * side effects!
     *
     * @param inputStreamOntology The inputStream of the last ontology
     * @param localVars task local variables that may be used during execution
     * @param defaultPathToOutFile default path that was specified on construction of the external worker associated with this strategy.
     * @return Path to produces output ontology that is used for following tasks. We try to read this file and persist it in the processing engine.
     */
    File executeReasoningLogic(InputStream inputStreamOntology, Map<String, String> localVars, File defaultPathToOutFile);
}
