package cz.gov.data.ms.sharepoint;

public class Cell {

    public final Column column;

    public final Object value;

    public Cell(Column column, Object value) {
        this.column = column;
        this.value = value;
    }

}
