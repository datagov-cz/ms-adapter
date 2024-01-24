package cz.gov.data.csvw;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

public interface RdfCollector {

    void type(Resource subject, String type);

    void url(Resource subject, String predicate, String object);

    void add(Resource subject, String predicate, Value object);

}
