package dk.statsbiblioteket.newspaper.adhoctitleingester;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class AdhocTitleIngesterTest {

    @BeforeMethod
    public void setUp() throws Exception {
        EnhancedFedora enhancedFedora = mock(EnhancedFedora.class);
    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testIngestDirectory() throws Exception {

    }

    @Test
    public void testIngestFiles() throws Exception {

    }

    @Test
    public void testIngestFile() throws Exception {

    }

    @Test
    public void testLinkPids() throws Exception {

    }

    @Test
    public void testPublishPids() throws Exception {
    }
}