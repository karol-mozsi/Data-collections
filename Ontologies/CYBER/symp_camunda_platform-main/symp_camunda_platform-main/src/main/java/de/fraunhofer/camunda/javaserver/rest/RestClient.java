package de.fraunhofer.camunda.javaserver.rest;

import de.fraunhofer.camunda.javaserver.exceptions.HTTPException;
import de.fraunhofer.camunda.javaserver.utils.PropertiesService;
import org.camunda.bpm.client.task.ExternalTask;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A RestClient that communicates with a running Camunda REST Server
 *
 */
public class RestClient {

    private static final String REST_URI = PropertiesService.getRestEndpoint();
    private final Client client;

    public RestClient() {
        client = ClientBuilder.newClient();
    }

    public Map<String, ? extends TypedVariable> getTaskLocalVariables(ExternalTask externalTask) {
        return client.target(REST_URI)
                     .path("execution/" + externalTask.getExecutionId() + "/localVariables")
                     .request(MediaType.APPLICATION_JSON)
                     .get()
                     .readEntity(new GenericType<Map<String, TypedVariable>>() {
                     });
    }

    public Object get(String getRequest) {
        return client.target(REST_URI)
                     .path(getRequest)
                     .request(MediaType.APPLICATION_JSON)
                     .get()
                     .readEntity(new GenericType<LinkedHashMap<String, String>>() {
                     });
    }

    public void deleteProcessVariable(String processInstanceId, String varName) throws HTTPException {
        Response response = delete("process-instance/ " + processInstanceId + "/variables/" + varName);
        if(response.getStatus() != 204) {
            throw new HTTPException("Delete returned unexpected response code!");
        }
    }

    public Response delete(String deleteRequest) {
        return client.target(REST_URI)
                     .path(deleteRequest)
                     .request()
                     .delete();
    }

    public List<String> getTaskLocalStrings(ExternalTask externalTask) {
        return extractStringList(getTaskLocalVariables(externalTask));
    }

    public Map<String, String> getTaskLocalStringMap(ExternalTask externalTask) {
        return extractStringMap(getTaskLocalVariables(externalTask));
    }

    public static List<String> extractStringList(Map<String, ? extends TypedVariable> m) {
        return new ArrayList<>(extractStringMap(m).values());
    }

    public static Map<String, String> extractStringMap(Map<String, ? extends TypedVariable> m) {
        return m.entrySet()
                .stream()
                .flatMap(RestClient::entryToStreamMapper)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static <T extends Map.Entry<?, ? extends TypedVariable>> Stream<Map.Entry<String, String>> entryToStreamMapper(T e) {
        TypedVariable v = e.getValue();
        if (v.getValue() instanceof Map) {
            return ((Map<String, String>) v.getValue()).entrySet()
                                                       .stream();
        } else if (v.getValue() instanceof List) {
            final AtomicInteger counter = new AtomicInteger(0);
            return ((List<String>) v.getValue()).stream()
                                                .collect(Collectors.toMap(s -> e.getKey() + "_" + counter.getAndIncrement(), s -> s))
                                                .entrySet()
                                                .stream();
        } else if (v.getValue() instanceof String) {
            Map<String, String> m = new HashMap<>();
            m.put(e.getKey()
                   .toString() + "_string", (String) v.getValue());
            return m.entrySet()
                    .stream();
        }
        return Stream.empty();
    }

}
