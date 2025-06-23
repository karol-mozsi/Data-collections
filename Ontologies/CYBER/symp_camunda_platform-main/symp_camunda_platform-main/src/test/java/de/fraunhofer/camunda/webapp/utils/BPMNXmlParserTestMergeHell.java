package de.fraunhofer.camunda.webapp.utils;

import de.fraunhofer.camunda.javaserver.utils.BPMNXmlParser;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class BPMNXmlParserTestMergeHell {

    private BPMNXmlParser xmlParser;

    @Before
    public void setup() {
        String resourcesPath = "src/test/resources/definitions/mergingHell.bpmn";
        File file = new File(resourcesPath);
        xmlParser = new BPMNXmlParser(file);
    }

    @Test
    public void getJoinableOntologies() {
        Collection<String> joinableOnts = xmlParser.getJoinableOnts("ServiceTask_0frfty3");
        System.out.println(joinableOnts);
        assertThat(joinableOnts, containsInAnyOrder(
                equalTo("ServiceTask_1hjwa5e"),
                equalTo("ServiceTask_0h7spuc"),
                equalTo("ServiceTask_0983one"),
                equalTo("ServiceTask_0lm1wge"),
                equalTo("ServiceTask_0f9tzd5")
        ));
        assertEquals(5, joinableOnts.size());
    }


}
