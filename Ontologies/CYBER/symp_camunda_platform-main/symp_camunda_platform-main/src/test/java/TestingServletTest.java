import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;

@Slf4j
public class TestingServletTest {

    private TestingServlet testingServlet;
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        testingServlet = new TestingServlet();
        gson = new Gson();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getTopicsAsString() {
        List actual = gson.fromJson(testingServlet.getTopicsAsString(), List.class);
        List expected = gson.fromJson("[\"SWRL\", \"JENA\", \"ONTUPLOAD\", \"ONTMERGE\", \"NOT_A_SERVICETASK\",\"LOGGERTEST\", \"GENERICREASONING\"]", List.class);
        assertArrayEquals(actual.toArray(), expected.toArray());
        log.info(actual.toString());
    }
}
