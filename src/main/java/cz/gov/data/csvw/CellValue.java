package cz.gov.data.csvw;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;

public interface CellValue {

    boolean isList();

    Value getValue();

    Collection<Value> getValues();

}
