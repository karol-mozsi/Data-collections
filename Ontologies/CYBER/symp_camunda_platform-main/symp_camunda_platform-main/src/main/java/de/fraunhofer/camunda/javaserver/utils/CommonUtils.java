package de.fraunhofer.camunda.javaserver.utils;

import static org.camunda.spin.Spin.JSON;

import org.camunda.spin.json.SpinJsonNode;

public class CommonUtils {
	public static SpinJsonNode addToCurrentOntologies(SpinJsonNode jsonCurrentOntologies, String taskId, String path) {
        return jsonCurrentOntologies.append(JSON("{\"activityId\":\"" + taskId 
        		+ "\", \"path\":\"" + path + "\"}"));
	}
}
