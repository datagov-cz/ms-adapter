package cz.gov.data.ms.sharepoint;

public class SharepointFile {

    public final String siteIdentifier;

    public final String fileIdentifier;

    public final String fileName;

    public SharepointFile(String siteIdentifier, String fileIdentifier, String fileName) {
        this.siteIdentifier = siteIdentifier;
        this.fileIdentifier = fileIdentifier;
        this.fileName = fileName;
    }

}
