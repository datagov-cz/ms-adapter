package cz.gov.data.ms.sharepoint;

public class Column {

    public final ColumnType type;

    /**
     * Stable uniq identifier.
     */
    public final String name;

    /**
     * Can be language specific, do not use to identify a column.
     */
    public final String label;

    public Column(ColumnType type, String name, String label) {
        this.type = type;
        this.name = name;
        this.label = label;
    }

}
