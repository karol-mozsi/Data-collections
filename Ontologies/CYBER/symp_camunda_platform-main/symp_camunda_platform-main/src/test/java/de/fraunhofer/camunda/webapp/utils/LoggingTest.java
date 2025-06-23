package de.fraunhofer.camunda.webapp.utils;

import org.junit.Test;

import java.util.Arrays;

public class LoggingTest {

    private static final String replacementPattern = "\\{}";

    @Test
    public void testStringReplacement() {
        String msg = "This is a test message {}, {}";
        String[] args = {"test1", "te\\setset\\st2"};

        String res = msg;
        for (String a : args) {
            a = a.replaceAll("\\\\", "\\\\\\\\lol");
            res = res.replaceFirst(replacementPattern, a);
        }
        System.out.println("[message]: " + res);
        System.out.println(Arrays.toString(args));

    }
}
