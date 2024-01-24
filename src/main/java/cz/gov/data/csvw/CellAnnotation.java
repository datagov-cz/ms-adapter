package cz.gov.data.csvw;

public class CellAnnotation {

    public String aboutUrl;

    public String propertyUrl;

    public String name;

    public static CellAnnotation empty() {
        return new CellAnnotation(null, null, null);
    }

    public CellAnnotation(String aboutUrl, String propertyUrl, String name) {
        this.aboutUrl = aboutUrl;
        this.propertyUrl = propertyUrl;
        this.name = name;
    }

}
