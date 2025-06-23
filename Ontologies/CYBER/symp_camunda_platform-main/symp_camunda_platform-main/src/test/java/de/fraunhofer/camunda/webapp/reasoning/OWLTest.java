package de.fraunhofer.camunda.webapp.reasoning;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Getter
public abstract class OWLTest {

    public static final File ONTOLOGY_FILE = new File("src/test/resources", "test.owl");
    public static final File BACKUP_ONTOLOGY_FILE = new File("src/test/resources", "testBackup.owl");

    @Before
    public void backupTestOwl() throws IOException {
        copyAndCheck(ONTOLOGY_FILE, BACKUP_ONTOLOGY_FILE);
    }

    @After
    public void restoreTestOwl() throws IOException {
        copyAndCheck(BACKUP_ONTOLOGY_FILE, ONTOLOGY_FILE);
    }

    private void copyAndCheck(File input, File output) throws IOException {
        FileUtils.copyFile(input, output);
        assertTrue(output.exists());
        assertEquals(Files.readAllLines(input.toPath()), Files.readAllLines(output.toPath()));
    }
}
