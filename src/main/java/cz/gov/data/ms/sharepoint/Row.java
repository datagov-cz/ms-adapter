package cz.gov.data.ms.sharepoint;

import java.util.Collections;
import java.util.List;

public class Row {

    public final List<Cell> cells;

    public Row(List<Cell> cells) {
        this.cells = Collections.unmodifiableList(cells);
    }

}
