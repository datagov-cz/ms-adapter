package cz.gov.data.ms.sharepoint;

import java.util.Collections;
import java.util.List;

public class SharepointList {

    public final List<Column> columns;

    public final List<Row> rows;

    public SharepointList(List<Column> columns, List<Row> rows) {
        this.columns = Collections.unmodifiableList(columns);
        this.rows = Collections.unmodifiableList(rows);
    }
}
