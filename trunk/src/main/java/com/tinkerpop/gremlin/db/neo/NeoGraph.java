package com.tinkerpop.gremlin.db.neo;

import com.tinkerpop.gremlin.model.Edge;
import com.tinkerpop.gremlin.model.Graph;
import com.tinkerpop.gremlin.model.Index;
import com.tinkerpop.gremlin.model.Vertex;
import com.tinkerpop.gremlin.statements.EvaluationException;
import org.neo4j.api.core.*;
import org.neo4j.util.index.LuceneIndexService;

import java.util.Iterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @version 0.1
 */
public class NeoGraph implements Graph {

    private NeoService neo;
    private NeoIndex index;
    private Transaction tx;

    public NeoGraph(String directory) {
        this.neo = new EmbeddedNeo(directory);
        this.index = new NeoIndex(new LuceneIndexService(neo));

        tx = neo.beginTx();
        try {
            Node referenceNode = this.neo.getReferenceNode();
            referenceNode.delete();
            this.stopStartTransaction();
        } catch (NotFoundException e) {
        }
    }

    public Index getIndex() {
        return this.index;
    }

    public Vertex addVertex(Object id) {
        try {
            if (null != id) {
                Long longId = Double.valueOf(id.toString()).longValue();
                return new NeoVertex(this.neo.getNodeById(longId), this.index);
            } else {
                throw new NotFoundException();
            }
        } catch (NotFoundException e) {
            Vertex vertex = new NeoVertex(neo.createNode(), this.index);
            this.stopStartTransaction();
            return vertex;
        } catch (NumberFormatException e) {
            throw new EvaluationException("Neo vertex ids must be convertible to a long value.");
        }
    }

    public Vertex getVertex(Object id) {
        try {
            Long longId = Double.valueOf(id.toString()).longValue();
            Node node = this.neo.getNodeById(longId);
            return new NeoVertex(node, this.index);
        } catch (NotFoundException e) {
            return null;
        } catch (NumberFormatException e) {
            throw new EvaluationException("Neo vertex ids must be convertible to a long value.");
        }
    }

    public Iterator<Vertex> getVertices() {
        return new NeoVertexIterator(this.neo.getAllNodes().iterator());
    }

    public Iterator<Edge> getEdges() {
        return new NeoEdgeIterator(this.neo.getAllNodes().iterator());
    }

    public void removeVertex(Vertex vertex) {
        Long id = (Long) vertex.getId();
        Node node = neo.getNodeById(id);
        if (null != node) {
            for (String key : vertex.getPropertyKeys()) {
                this.index.remove(key, vertex.getProperty(key), vertex);
            }
            for(Edge edge : vertex.getBothEdges()) {
                this.removeEdge(edge);
            }
            node.delete();
            this.stopStartTransaction();
        }
    }

    public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        Node outNode = (Node)((NeoVertex)outVertex).getRawElement();
        Node inNode = (Node)((NeoVertex)inVertex).getRawElement();
        Relationship relationship = outNode.createRelationshipTo(inNode, DynamicRelationshipType.withName(label));
        this.stopStartTransaction();
        return new NeoEdge(relationship, this.index);
    }

    public void removeEdge(Edge edge) {
        ((Relationship) ((NeoEdge) edge).getRawElement()).delete();
        this.stopStartTransaction();
    }

    private void stopStartTransaction() {
        tx.success();
        tx.finish();
        tx = neo.beginTx();
    }

    public void shutdown() {
        this.tx.success();
        this.tx.finish();
        this.neo.shutdown();
        this.index.shutdown();

    }

    private class NeoVertexIterator implements Iterator<Vertex> {

        Iterator<Node> nodes;

        public NeoVertexIterator(Iterator<Node> nodes) {
            this.nodes = nodes;
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public Vertex next() {
            return new NeoVertex(this.nodes.next(), index);
        }

        public boolean hasNext() {
            return this.nodes.hasNext();
        }
    }

    private class NeoEdgeIterator implements Iterator<Edge> {

        Iterator<Node> nodes;
        Iterator<Relationship> nodeRelationships;


        public NeoEdgeIterator(Iterator<Node> nodes) {
            this.nodes = nodes;
            if (this.nodes.hasNext()) {
                this.nodeRelationships = nodes.next().getRelationships(Direction.OUTGOING).iterator();
            }
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public Edge next() {
            if (nodeRelationships.hasNext())
                return new NeoEdge(nodeRelationships.next(), index);
            else if (this.nodes.hasNext()) {
                this.nodeRelationships = nodes.next().getRelationships(Direction.OUTGOING).iterator();
                return next();
            } else {
                return null;
            }
        }

        public boolean hasNext() {
            if (!this.nodes.hasNext() && !this.nodeRelationships.hasNext())
                return false;
            else
                return true;
        }

    }
}
