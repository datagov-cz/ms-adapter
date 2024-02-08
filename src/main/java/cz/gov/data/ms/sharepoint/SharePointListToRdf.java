package cz.gov.data.ms.sharepoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
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
import java.util.Map;

public class SharePointListToRdf {

    public interface Handler {

        void handle(ValueFactory valueFactory, JsonElement value, ValueHolder target);

    }

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

    protected final CellAnnotation cellAnnotation = CellAnnotation.empty();

    protected final ValueHolder cellValue = new ValueHolder();

    protected StatementsBuilder collector;

    protected Map<String, Handler> customHandlers;

    public static List<Statement> toRdf(
            SharepointList list,
            String url,
            Map<String, Handler> customHandlers) {
        int expectedSize = (list.columns.size() + 3) * list.rows.size();
        var result = new ArrayList<Statement>(expectedSize);
        var collector = new StatementsBuilder(result);
        (new SharePointListToRdf(collector, customHandlers))
                .listToRdf(list, url);
        return result;
    }

    public SharePointListToRdf(
            StatementsBuilder collector,
            Map<String, Handler> customHandlers) {
        this.collector = collector;
        this.customHandlers = customHandlers;
    }

    protected void listToRdf(SharepointList list, String url) {
        var adapter = CsvwToRdf.minimalMode(collector);
        var annotation = TableAnnotation.empty();
        annotation.url = url;
        adapter.onTable(null, annotation);
        for (Row row : list.rows) {
            adapter.onRow(null);
            for (Cell cell : row.cells) {
                prepareCellData(cell);
                adapter.onCell(cellAnnotation, cellValue);
            }
        }
    }

    protected void prepareCellData(Cell cell) {
        cellAnnotation.name = cell.column.name;
        // Custom handler.
        var handler = customHandlers.get(cell.column.label);
        if (handler != null) {
            handler.handle(valueFactory, cell.value, cellValue);
            return;
        }
        //
        JsonElement element = cell.value;
        if (element == null || element.isJsonNull()) {
            cellValue.set();
        } else if (element.isJsonPrimitive()) {
            var value = jsonToRdf(element.getAsJsonPrimitive());
            cellValue.set(value);
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            var value = new ArrayList<Value>(array.size());
            for (JsonElement item : array) {
                value.add(jsonToRdf(item));
            }
            cellValue.set(value);
        } else if (element.isJsonObject()) {
            throw new UnsupportedOperationException(
                    "Can not convert JSON object.");
        }
    }

    protected Value jsonToRdf(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        } else if (value.isJsonPrimitive()) {
            return jsonToRdf(value.getAsJsonPrimitive());
        } else if (value.isJsonArray()) {
            throw new UnsupportedOperationException(
                    "Can not convert JSON array.");
        } else if (value.isJsonObject()) {
            throw new UnsupportedOperationException(
                    "Can not convert JSON object.");
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected Value jsonToRdf(JsonPrimitive value) {
        if (value.isNumber()) {
            return valueFactory.createLiteral(value.getAsInt());
        } else if (value.isBoolean()) {
            return valueFactory.createLiteral(value.getAsBoolean());
        } else {
            return valueFactory.createLiteral(value.getAsString());
        }
    }

}
