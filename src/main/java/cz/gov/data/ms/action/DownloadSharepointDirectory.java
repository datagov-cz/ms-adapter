package cz.gov.data.ms.action;

import com.microsoft.graph.requests.GraphServiceClient;
import cz.gov.data.ms.AzureAuthentication;
import cz.gov.data.ms.sharepoint.SharepointFile;
import cz.gov.data.ms.sharepoint.Sharepoint;
import cz.gov.data.ms.sharepoint.SharepointException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DownloadSharepointDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadSharepointDirectory.class);

    protected final AzureAuthentication authentication;

    protected final String siteIdentifier;

    public static void downloadContent(
            AzureAuthentication authentication,
            String siteIdentifier,
            String driveName,
            String directoryName,
            Path outputPath) {
        var instance = new DownloadSharepointDirectory(
                authentication, siteIdentifier);
        try {
            Files.createDirectories(outputPath);
        } catch (IOException ex) {
            LOG.error("Can not create target directory '{}'.", outputPath, ex);
        }
        instance.downloadList(driveName, directoryName, outputPath);
    }

    public DownloadSharepointDirectory(
            AzureAuthentication authentication, String siteIdentifier) {
        this.authentication = authentication;
        this.siteIdentifier = siteIdentifier;
    }

    protected void downloadList(
            String driveName, String directoryName, Path outputPath) {
        var graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(authentication.provider())
                .buildClient();
        var sharepoint = new Sharepoint<>(graphClient);
        List<SharepointFile> fileList;

        try {
            fileList = sharepoint.listDriveDirectory(
                    siteIdentifier, driveName, directoryName);
        } catch (SharepointException ex) {
            LOG.error("Can not list content of a directory '{}' drive '{}'",
                    directoryName, driveName, ex);
            return;
        }

        for (SharepointFile driveFile : fileList) {
            try {
                sharepoint.downloadFile(driveFile, outputPath);
            } catch (IOException | SharepointException ex) {
                LOG.error("Can not download file '{}'", driveFile.fileName, ex);
            }
        }
    }
}
