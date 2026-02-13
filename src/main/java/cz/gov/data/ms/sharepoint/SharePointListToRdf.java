package cz.gov.data.ms.sharepoint;

import com.microsoft.kiota.serialization.*;
import cz.gov.data.csvw.CellAnnotation;
import cz.gov.data.csvw.CellValue;
import cz.gov.data.csvw.CsvwToRdf;
import cz.gov.data.csvw.TableAnnotation;
import cz.gov.data.rdf.StatementsBuilder;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SharePointListToRdf {

    public static class ValueHolder implements CellValue {

        private Value value;

        private Collection<Value> values;

        @Override
        public boolean isList() {
            return values != null;
        }

        @Override
        public Value getValue() {
            return value;
        }

        @Override
        public Collection<Value> getValues() {
            return values;
        }

        public void set() {
            this.value = null;
            this.values = null;
        }

        public void set(Value value) {
            this.value = value;
            this.values = null;
        }

        public void set(Collection<Value> values) {
            this.value = null;
            this.values = values;
        }

    }

    protected final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    protected StatementsBuilder collector;

    public static List<Statement> toRdf(SharepointList list, String url) {
        int expectedSize = (list.columns.size() + 3) * list.rows.size();
        var result = new ArrayList<Statement>(expectedSize);
        var collector = new StatementsBuilder(result);
        (new SharePointListToRdf(collector)).listToRdf(list, url);
        return result;
    }

    public SharePointListToRdf(StatementsBuilder collector) {
        this.collector = collector;
    }

    protected void listToRdf(SharepointList list, String url) {
        var adapter = CsvwToRdf.minimalMode(collector);
        var annotation = TableAnnotation.empty();
        annotation.url = url;
        adapter.onTable(null, annotation);
        // To not recreate every time we keep the values here.
        var cellValues = new ArrayList<Value>();
        var cellAnnotation = CellAnnotation.empty();
        //
        for (Row row : list.rows) {
            adapter.onRow(null);
            for (Cell cell : row.cells) {
                // Prepare metadata
                cellAnnotation.name = cell.column.name;
                // Prepare value
                cellValues.clear();
                prepareCellValue(cell, cellValues);
                adapter.onCell(cellAnnotation, cellValues);
            }
        }
    }

    protected void prepareCellValue(Cell cell, Collection<Value> values) {
        // Try to process as a primitive value.
        if (processPrimitiveValue(cell.value, values)) {
            return;
        } else if (cell.value instanceof UntypedArray typed) {
            Iterable<UntypedNode> items = typed.getValue();
            for (UntypedNode item : items) {
                if (processPrimitiveValue(item, values)) {
                    continue;
                }
                throw new UnsupportedOperationException("Unknown type :'" + item.getClass().getName() + "'.");
            }
            return;
        }
        throw new UnsupportedOperationException("Unknown type :'" + cell.value.getClass().getName() + "'.");
    }

    protected boolean processPrimitiveValue(Object value, Collection<Value> values) {
        switch (value) {
            case null -> {
                // Do nothing.
            }
            case String typed -> {
                values.add(valueFactory.createLiteral(typed));
            }
            case Boolean typed -> {
                values.add(valueFactory.createLiteral(typed));
            }
            case UntypedString typed -> {
                values.add(valueFactory.createLiteral(typed.getValue()));
            }
            case UntypedBoolean typed -> {
                values.add(valueFactory.createLiteral(typed.getValue()));
            }
            case UntypedInteger typed -> {
                values.add(valueFactory.createLiteral(typed.getValue()));
            }
            case UntypedDouble typed -> {
                values.add(valueFactory.createLiteral(typed.getValue()));
            }
            case UntypedNull untypedNull -> {
                // Do nothing.
            }
            default -> {
                return false;
            }
        }
        return true;
    }

}
