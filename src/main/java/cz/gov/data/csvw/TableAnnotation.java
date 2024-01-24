package cz.gov.data.csvw;

public class TableAnnotation {

    public String url;

    public static TableAnnotation empty() {
        return new TableAnnotation(null);
    }

    public TableAnnotation(String url) {
        this.url = url;
    }

}
