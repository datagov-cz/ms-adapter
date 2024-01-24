package cz.gov.data.ms.sharepoint;

import com.google.gson.JsonElement;

public class Cell {

    public final Column column;

    public final JsonElement value;

    public Cell(Column column, JsonElement value) {
        this.column = column;
        this.value = value;
    }

}
