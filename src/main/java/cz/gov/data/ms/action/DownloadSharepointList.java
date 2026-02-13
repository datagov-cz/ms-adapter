package cz.gov.data.ms.action;

import cz.gov.data.ms.sharepoint.SharePointListToRdf;
import cz.gov.data.ms.AzureAuthentication;
import cz.gov.data.ms.sharepoint.Sharepoint;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.trig.TriGWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class DownloadSharepointList{

    private static final Logger LOG = LoggerFactory.getLogger(DownloadSharepointList.class);

    protected final AzureAuthentication authentication;

    protected final String siteIdentifier;

    public static void downloadContent(
            AzureAuthentication authentication,
            String siteIdentifier,
            String listIdentifier,
            String baseUrl,
            Path outputPath)  {
        var instance = new DownloadSharepointList(authentication, siteIdentifier);
        instance.downloadList(listIdentifier, baseUrl, outputPath);
    }

    public DownloadSharepointList(
            AzureAuthentication authentication, String siteIdentifier) {
        this.authentication = authentication;
        this.siteIdentifier = siteIdentifier;
    }

    protected void downloadList(
            String listIdentifier,
            String baseUrl,
            Path outputPath) {
        var sharepointList = new Sharepoint(authentication.graphClient())
                .downloadList(siteIdentifier, listIdentifier);
        var statements = SharePointListToRdf.toRdf(sharepointList, baseUrl);
        try {
            write(statements, outputPath);
        } catch(IOException exception) {
            LOG.error("Failed to save list to '{}'.", outputPath, exception);
        }
    }

    protected void write(
            List<Statement> statements,
            Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        LOG.info("Saving {} statements to {}", statements.size(), outputPath);
        try (var writer = Files.newBufferedWriter(
                outputPath, StandardCharsets.UTF_8)) {
            var trigWriter = new TriGWriter(writer);
            trigWriter.startRDF();
            for (Statement statement : statements) {
                trigWriter.handleStatement(statement);
            }
            trigWriter.endRDF();
        }
    }

}
