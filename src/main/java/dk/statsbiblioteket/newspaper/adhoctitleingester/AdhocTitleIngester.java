package dk.statsbiblioteket.newspaper.adhoctitleingester;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.ChecksumType;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.central.connectors.fedora.templates.ObjectIsWrongTypeException;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Read a list of files, and for each file clone a newspaper template, then add the file as DOMS datastream, and link
 * the previously cloned file to this file.
 */
public class AdhocTitleIngester {
    //Datamodel to use
    private static final String NEWSPAPER_TEMPLATE = "doms:Template_Newspaper";
    private static final String NEWSPAPER_DATASTREAM = "MODS";
    private static final String NEWSPAPER_RELATION
            = "http://doms.statsbiblioteket.dk/relations/default/0/1/#isPartOfNewspaper";
    private static final String LOG_MESSAGE = "Adding newspaper title";

    private static final String FEDORA_URI_PREFIX = "info:fedora/";

    public AdhocTitleIngester(Properties properties) {
    }

    public static void main(String... args)
            throws IOException, PIDGeneratorException, JAXBException, BackendMethodFailedException,
            ObjectIsWrongTypeException, BackendInvalidResourceException, BackendInvalidCredsException {
        if (args.length < 1 ||args.length > 2) {
            System.err.println(
                    String.format("Usage: %s <directory> [propertiesfile]", AdhocTitleIngester.class.getName()));
            System.exit(1);
        }
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (args.length > 1) {
            properties.load(new FileReader(args[1]));
        }
        AdhocTitleIngester adhocTitleIngester = new AdhocTitleIngester(properties);
        String domsUsername = properties.getProperty("doms.username", "fedoraAdmin");
        String domsPassword = properties.getProperty("doms.password", "fedoraAdminPass");
        String domsUrl = properties.getProperty("doms.url", "http://achernar:7880/fedora");
        String domsPidgeneratorUrl = properties.getProperty("doms.pidgenerator.url", "http://achernar:7880/pidgenerator-service");
        String sourceCharset = properties.getProperty("source.charset", "cp1252");
        List<String> pids = adhocTitleIngester.ingestDirectory(args[0], new EnhancedFedoraImpl(
                new Credentials(domsUsername, domsPassword), domsUrl, domsPidgeneratorUrl, null), sourceCharset);
        System.out.println(pids);
    }

    public List<String> ingestDirectory(String sourceDirectory, EnhancedFedoraImpl enhancedFedora, String sourceCharset)
            throws IOException, PIDGeneratorException, JAXBException, BackendMethodFailedException,
            ObjectIsWrongTypeException, BackendInvalidResourceException, BackendInvalidCredsException {
        File[] files = new File(sourceDirectory).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        Arrays.sort(files);
        List<String> pids = ingestFiles(files, enhancedFedora, sourceCharset);
        linkPids(pids, enhancedFedora);
        publishPids(pids, enhancedFedora);
        return pids;
    }

    protected List<String> ingestFiles(File[] files, EnhancedFedoraImpl enhancedFedora, String sourceCharset)
            throws IOException, BackendInvalidCredsException, BackendMethodFailedException, ObjectIsWrongTypeException,
            BackendInvalidResourceException, PIDGeneratorException {
        List<String> pids = new ArrayList<>();
        for (File file : files) {
            pids.add(ingestFile(enhancedFedora, file, sourceCharset));
        }
        return pids;
    }

    protected String ingestFile(EnhancedFedoraImpl enhancedFedora, File file, String sourceCharset)
            throws IOException, BackendInvalidCredsException, BackendMethodFailedException, ObjectIsWrongTypeException,
            BackendInvalidResourceException, PIDGeneratorException {
        byte[] bytes = new String(Files.readAllBytes(file.toPath()), sourceCharset).getBytes("UTF-8");

        String pid = enhancedFedora.cloneTemplate(NEWSPAPER_TEMPLATE, Arrays.asList("path:" + file.getName()), LOG_MESSAGE);
        enhancedFedora.modifyObjectLabel(pid, file.getName().replaceAll("\\.xml$", ""), LOG_MESSAGE);
        enhancedFedora.modifyDatastreamByValue(pid, NEWSPAPER_DATASTREAM, ChecksumType.MD5, null, bytes,
                                               Collections.<String>emptyList(), "text/xml", LOG_MESSAGE, null);
        return pid;
    }

    protected void linkPids(List<String> pids, EnhancedFedoraImpl enhancedFedora)
            throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        for (int i = 0; i < pids.size() - 1; i++) {
            String lastPid = pids.get(i);
            enhancedFedora.addRelation(lastPid, asURI(lastPid), NEWSPAPER_RELATION, asURI(pids.get(i + 1)), false,
                                       LOG_MESSAGE);
        }
    }

    protected void publishPids(List<String> pids, EnhancedFedoraImpl enhancedFedora)
            throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        for (String pid : pids) {
            enhancedFedora.modifyObjectState(pid, "A", LOG_MESSAGE);
        }
    }

    private static String asURI(String pid) {
        if (!pid.startsWith(FEDORA_URI_PREFIX)) {
            return FEDORA_URI_PREFIX + pid;
        }
        return pid;
    }
}
