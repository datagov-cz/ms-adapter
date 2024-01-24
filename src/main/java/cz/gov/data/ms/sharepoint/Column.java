package cz.gov.data.ms.sharepoint;

public class Column {

    public final ColumnType type;

    public final String name;

    public final String label;

    public Column(ColumnType type, String name, String label) {
        this.type = type;
        this.name = name;
        this.label = label;
    }

}
