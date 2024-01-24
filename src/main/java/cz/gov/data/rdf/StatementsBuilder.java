package cz.gov.data.rdf;

import cz.gov.data.csvw.RdfCollector;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Collection;

public class StatementsBuilder implements RdfCollector {

    protected final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    protected final Collection<Statement> collector;

    public StatementsBuilder(Collection<Statement> collector) {
        this.collector = collector;
    }

    @Override
    public void type(Resource subject, String type) {
        type(subject, valueFactory.createIRI(type));
    }

    @Override
    public void add(Resource subject, String predicate, Value object) {
        add(subject, valueFactory.createIRI(predicate), object);
    }

    @Override
    public void url(Resource subject, String predicate, String object) {
        add(subject, predicate, valueFactory.createIRI(object));
    }

    public void type(Resource subject, Value type) {
        add(subject, RDF.TYPE, type);
    }

    public void add(Resource subject, IRI predicate, Value object) {
        collector.add(valueFactory.createStatement(subject, predicate, object));
    }

}
