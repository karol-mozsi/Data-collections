package de.fraunhofer.camunda.webapp.rest;

import de.fraunhofer.camunda.javaserver.rest.RestClient;
import de.fraunhofer.camunda.javaserver.rest.TypedVariable;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;

public class RestClientTest {

    LinkedHashMap<String, TypedVariable> m;

    @Before
    public void setup() {
        m = new LinkedHashMap<>();

        TypedVariable t1 = new TypedVariable();
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("asdf", "45");
        t1.setValue(stringMap);

        TypedVariable t2 = new TypedVariable();
        String test = "testString1234";
        t2.setValue(test);

        TypedVariable t3 = new TypedVariable();
        List<String> l = new ArrayList<>();
        l.add("test4");
        l.add("test5");
        t3.setValue(l);

        m.put("t1", t1);
        m.put("t2", t2);
        m.put("t3", t3);
    }

    @Test
    public void testExtractStringMap() {
        Map<String, String> res = RestClient.extractStringMap(m);
        System.out.println(res);
        assertThat(res, Matchers.allOf(
                Matchers.hasEntry("asdf", "45"),
                Matchers.hasEntry("t2_string", "testString1234"),
                Matchers.hasEntry("t3_0", "test4"),
                Matchers.hasEntry("t3_1", "test5")
        ));
    }

    @Test
    public void testExtractStringList() {
        List<String> res = RestClient.extractStringList(m);
        System.out.println(res);
        assertThat(res, Matchers.containsInAnyOrder(
                Matchers.equalTo("45"),
                Matchers.equalTo("testString1234"),
                Matchers.equalTo("test4"),
                Matchers.equalTo("test5")));
    }
}
