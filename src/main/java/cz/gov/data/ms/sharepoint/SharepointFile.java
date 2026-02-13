package cz.gov.data.ms.sharepoint;

public class SharepointFile {

    public final String driveIdentifier;

    public final String fileIdentifier;

    public final String fileName;

    public SharepointFile(String driveIdentifier, String fileIdentifier, String fileName) {
        this.driveIdentifier = driveIdentifier;
        this.fileIdentifier = fileIdentifier;
        this.fileName = fileName;
    }

}
