package de.fraunhofer.camunda.javaserver.rest;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class TypedVariable {
    private Object value;
    private String type;
    private Map<String, Object> valueInfo = new HashMap<>();
    private List<String> stringList = new ArrayList<>();

    @Override
    public String toString() {
        return stringList.toString();
    }

}
