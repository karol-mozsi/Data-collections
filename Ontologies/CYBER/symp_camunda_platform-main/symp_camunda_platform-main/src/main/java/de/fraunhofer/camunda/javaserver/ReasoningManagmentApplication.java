package de.fraunhofer.camunda.javaserver;

import de.fraunhofer.camunda.javaserver.external.ExternalWorker;
import de.fraunhofer.camunda.javaserver.external.LoggerTestWorker;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyMappingWorker;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyMergeWorker;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyRemergeWorker;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyUploadWorker;
import de.fraunhofer.camunda.javaserver.external.reasoning.ExternalStrategies;
import de.fraunhofer.camunda.javaserver.external.reasoning.ExternalReasoningWorkerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.impl.ServletProcessApplication;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;

import javax.servlet.ServletContextEvent;
import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ProcessApplication("Reasoning Management App")
public class ReasoningManagmentApplication extends ServletProcessApplication {

    private static final File basePath;
    private static final int WORKERS_PER_TOPIC = 1;
    private static List<ExternalWorker> workers = new ArrayList<>();
    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    static {
        basePath = new File(System.getProperty("catalina.base")).getAbsoluteFile();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);
        log.info("Servlet context initialized 2");
        log.info("--------------------------------");
        ExternalWorker.setDefaultDir(new File(basePath, "webapps/reasoning_bpm_1.0.0"));
        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        startExternalWorkers();
    }

    private void startExternalWorkers() {
        log.debug("This is a debug message");
        log.info("Starting {} external workers in total. {} workers per topic", WORKERS_PER_TOPIC * 5, WORKERS_PER_TOPIC);
        File outPath = new File(basePath, "webapps/reasoning_bpm_1.0.0");


        for (int i = 0; i < WORKERS_PER_TOPIC; i++) {
            workers.add(ExternalReasoningWorkerBuilder.getInstance()
                                          .setOutputDirectory(outPath)
                                          .setWorkerId(String.valueOf(atomicInteger.getAndIncrement()))
                                          .setReasoningStrategy(ExternalStrategies.jenaStrategy())
                                          .setTopic("jena")
                                          .build());

            workers.add(ExternalReasoningWorkerBuilder.getInstance()
                                                      .setOutputDirectory(outPath)
                                                      .setWorkerId(String.valueOf(atomicInteger.getAndIncrement()))
                                                      .setReasoningStrategy(ExternalStrategies.swrlStrategy())
                                                      .setTopic("swrl")
                                                      .build());
            
            workers.add(ExternalReasoningWorkerBuilder.getInstance()
            		.setOutputDirectory(outPath)
            		.setWorkerId(String.valueOf(atomicInteger.getAndIncrement()))
                    .setReasoningStrategy(ExternalStrategies.serviceMergerStrategy())
                    .setTopic("service_merger")
                    .build());

            workers.add(new LoggerTestWorker(String.valueOf(atomicInteger.getAndIncrement()), outPath));
            workers.add(new ExternalOntologyUploadWorker(String.valueOf(atomicInteger.getAndIncrement()), outPath));
            workers.add(new ExternalOntologyMergeWorker(String.valueOf(atomicInteger.getAndIncrement()), outPath));
            workers.add(new ExternalOntologyRemergeWorker(String.valueOf(atomicInteger.getAndIncrement()), outPath));
            workers.add(new ExternalOntologyMappingWorker(String.valueOf(atomicInteger.getAndIncrement()), outPath));
        }

        Timer t = new Timer();
        t.scheduleAtFixedRate(new WorkerTimer(), 0, 60000);

        log.info("Started external workers");
    }

    private static class WorkerTimer extends TimerTask {

        @Override
        public void run() {
            System.out.println("#DEBUG " + LocalTime.now() + " worker cardinality: " + workers.size() + ", workers: " + workers);
        }
    }

}
