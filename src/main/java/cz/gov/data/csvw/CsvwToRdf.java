package cz.gov.data.csvw;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * <a href="https://www.w3.org/TR/2015/REC-csv2rdf-20151217/">csv2rdf</a>
 */
public class CsvwToRdf {

    protected final String CSVW = "ttp://www.w3.org/ns/csvw#";

    protected final String TABLE_GROUP = CSVW + "TableGroup";

    protected final String HAS_TABLE = CSVW + "table";

    protected final String TABLE = CSVW + "Table";

    protected final String HAS_URL = CSVW + "url";

    protected final String HAS_ROW = CSVW + "row";

    protected final String ROW = CSVW + "Row";

    protected final String HAS_ROW_NUMBER = CSVW + "rownum";

    protected final String HAS_DESCRIBES = CSVW + "describes";

    protected final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    protected final RdfCollector collector;

    protected final boolean standardMode;

    protected Resource tableGroup;

    protected Resource table;

    protected TableAnnotation tableAnnotation;

    protected int rowNumber;

    protected Resource row;

    protected Resource defaultCell;

    protected Resource cell;

    public static CsvwToRdf standardMode(RdfCollector collector) {
        return new CsvwToRdf(collector, true);
    }

    public static CsvwToRdf minimalMode(RdfCollector collector) {
        return new CsvwToRdf(collector, false);
    }

    protected CsvwToRdf(RdfCollector collector, boolean standardMode) {
        this.collector = collector;
        this.standardMode = standardMode;
    }

    public void onTableGroup(String identifier) {
        if (standardMode) {
            // 1. In standard mode only, establish a new node G.
            // If the group of tables has an identifier then node G
            // must be identified accordingly; else if identifier is null,
            // then node G must be a new blank node.
            if (identifier == null) {
                tableGroup = valueFactory.createBNode();
            } else {
                tableGroup = valueFactory.createIRI(identifier);
            }

            // 2. In standard mode only, specify the type of
            // node G as csvw:TableGroup; emit the following triple:
            collector.type(tableGroup, TABLE_GROUP);

            // 3. In standard mode only, emit the triples generated by running
            // the algorithm specified in section 6. JSON-LD to RDF over any
            // notes and non-core annotations specified for the group of tables,
            // with node G as an initial subject, the notes or non-core
            // annotation as property, and the value of the notes or non-core
            // annotation as value.
            // TODO
        }
    }

    public void onTable(String identifier, TableAnnotation annotation) {
        // 4. For each table where the suppress output annotation is false:
        if (standardMode) {
            // 4.1 In standard mode only, establish a new node T which
            // represents the current table.
            if (identifier == null) {
                table = valueFactory.createBNode();
            } else {
                table = valueFactory.createIRI(identifier);
            }
            // 4.2 In standard mode only, relate the table to the group of
            // tables; emit the following triple:
            collector.add(tableGroup, HAS_TABLE, table);

            // 4.3 In standard mode only, specify the type of node
            // T as csvw:Table; emit the following triple:
            collector.type(table, TABLE);

            // 4.4 In standard mode only, specify the source tabular data
            // file URL for the current table based on the url annotation;
            // emit the following triple:
            if (annotation.url != null) {
                collector.url(table, HAS_URL, annotation.url);
            }

            // 4.5 In standard mode only, emit the triples generated by running
            // the algorithm specified in section 6. JSON-LD to RDF over any
            // notes and non-core annotations specified for the table, with
            // node T as an initial subject, the notes or non-core annotation
            // as property, and the value of the notes or non-core annotation
            // as value.
            // TODO

        }

        // Prepare data for a new table.
        this.rowNumber = 0;
        this.tableAnnotation = annotation;
    }

    public void onRow(Integer rowSourceNumber) {
        // 4.6 For each row in the current table:
        ++rowNumber;

        if (standardMode) {
            // 4.6.1 In standard mode only, establish a new blank node R
            // which represents the current row.
            row = valueFactory.createBNode();

            // 4.6.2 In standard mode only, relate the row to the table;
            // emit the following triple:
            collector.add(table, HAS_ROW, row);

            // 4.6.3 In standard mode only, specify the type of node R
            // as csvw:Row; emit the following triple:
            collector.type(row, ROW);

            // 4.6.4 In standard mode only, specify the row number n for
            // the row; emit the following triple:
            collector.add(row, HAS_ROW_NUMBER,
                    valueFactory.createLiteral(rowNumber));

            // 4.6.5 In standard mode only, specify the row source number
            // n_source for the row within the source tabular data file URL
            // using a fragment-identifier as specified in [RFC7111];
            // if row source number is not null, emit the following triple:
            if (rowSourceNumber != null) {
                String url = table.stringValue() + "#row=" + rowSourceNumber;
                collector.url(row, HAS_URL, url);
            }

            // 4.6.6 In standard mode only, if row titles is not null,
            // insert any titles specified for the row. For each value, tv,
            // of the row titles annotation, emit the following triple:
            // TODO

            // 4.6.7 In standard mode only, emit the triples generated by
            // running the algorithm specified in section 6. JSON-LD to RDF
            // over any non-core annotations specified for the row, with node
            // R as an initial subject, the non-core annotation as property,
            // and the value of the non-core annotation as value.
            // TODO

        }

        // 4.6.8 Establish a new blank node S_def to be used
        // as the default subject for cells where about URL is undefined.
        // ...
        defaultCell = valueFactory.createBNode();
    }

    public void onCell(CellAnnotation annotation, CellValue value) {
        // 4.6.8 ... For each cell in the current row where the suppress output
        // annotation for the column associated with that cell is false:

        // 4.6.8.1 Establish a node S from about URL if set,
        // or from S_def otherwise as the current subject.
        if (annotation.aboutUrl == null) {
            cell = defaultCell;
        } else {
            cell = valueFactory.createIRI(annotation.aboutUrl);
        }

        if (standardMode) {
            // 4.6.8.2 In standard mode only, relate the current subject to
            // the current row; emit the following triple:
            collector.add(row, HAS_DESCRIBES, cell);
        }

        // 4.6.8.3 If the value of property URL for the cell is not null
        // then predicate P takes the value of property URL.
        // Else, predicate P is constructed by appending the value of the name
        // annotation for the column associated with the cell to the
        // tabular data file URL as a fragment identifier.
        String predicate;
        if (annotation.propertyUrl != null) {
            predicate = annotation.propertyUrl;
        } else {
            predicate = tableAnnotation.url + "#" + annotation.name;
        }

        // 4.6.8.4 If the value URL for the current cell is not null, then
        // value URL identifies a node V_url that is related the current subject
        // using the predicate P; emit the following triple:
        // TODO

        // 4.6.8.5 Else, if the cell value is a list and the cell ordered
        // annotation is true, then the cell value provides an ordered
        // sequence of literal nodes for inclusion within the RDF output
        // using an instance of rdf:List V_list as defined in [rdf-schema].
        // This instance is related to the subject using the predicate P;
        // emit the triples defining list V_list plus the following triple:
        // TODO

        if (value.isList()) {
            // 4.6.8.6 Else, if the cell value is a list, then the cell value
            // provides an unordered sequence of literal nodes for inclusion
            // within the RDF output, each of which is related to the subject
            // using the predicate P. For each value provided in the sequence,
            // add a literal node V_literal; emit the following triple:
            for (Value item : value.getValues()) {
                collector.add(cell, predicate, item);
            }
        } else if (value.getValue() != null) {
            // 4.6.8.7 Else, if the cell value is not null, then the cell value
            // provides a single literal node V_literal for inclusion within the
            // RDF output that is related the current subject using the
            // predicate P; emit the following triple:
            //
            // The literal nodes derived from the cell values must be expressed
            // according to the cell value's datatype as defined below:
            // Interpreting datatypes.
            collector.add(cell, predicate, value.getValue());
        }
    }

}
