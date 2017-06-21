package org.opentripplanner.inspector.tileRenderer.graphRenderer;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Created by rikde on 21/06/2017.
 */
public interface GraphRenderer {

    /**
     * @param e The edge being rendered.
     * @param attrs The edge visual attributes to fill-in.
     * @return True to render this edge, false otherwise.
     */
    public abstract boolean renderEdge(Edge e, EdgeVisualAttributes attrs);

    /**
     * @param v The vertex being rendered.
     * @param attrs The vertex visual attributes to fill-in.
     * @return True to render this vertex, false otherwise.
     */
    public abstract boolean renderVertex(Vertex v, VertexVisualAttributes attrs);

    /**
     * Name of this tile Render which would be shown in frontend
     *
     * @return Name of tile render
     */
    public abstract String getName();
}