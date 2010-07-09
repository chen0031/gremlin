package com.tinkerpop.gremlin.compiler.functions.sail;

import com.tinkerpop.blueprints.pgm.impls.sail.SailGraph;
import com.tinkerpop.gremlin.compiler.Atom;
import com.tinkerpop.gremlin.compiler.functions.AbstractFunction;
import com.tinkerpop.gremlin.compiler.functions.FunctionHelper;
import com.tinkerpop.gremlin.compiler.operations.Operation;

import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AddNamespaceFunction extends AbstractFunction<Boolean> {

    private final String FUNCTION_NAME = "add-ns";

    public Atom<Boolean> compute(List<Operation> parameters) throws RuntimeException {

        final int size = parameters.size();
        final SailGraph graph = (SailGraph) FunctionHelper.getGraph(parameters, 0);

        final String prefix;
        final String namespace;

        if (size == 2) {
            prefix = (String) parameters.get(0).compute().getValue();
            namespace = (String) parameters.get(1).compute().getValue();
        } else if (size == 3) {
            prefix = (String) parameters.get(1).compute().getValue();
            namespace = (String) parameters.get(2).compute().getValue();
        } else {
            throw new RuntimeException(this.createUnsupportedArgumentMessage());
        }
        graph.addNamespace(prefix, namespace);
        return new Atom<Boolean>(true);
    }

    public String getFunctionName() {
        return this.FUNCTION_NAME;
    }
}
