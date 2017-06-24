/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.inspector.tileRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.inspector.TileRenderer;
import org.opentripplanner.inspector.tileRenderer.graphRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.tileRenderer.graphRenderer.GraphRenderer;
import org.opentripplanner.inspector.tileRenderer.graphRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import com.jhlabs.awt.ShapeStroke;
import com.jhlabs.awt.TextStroke;
import com.vividsolutions.jts.awt.IdentityPointTransformation;
import com.vividsolutions.jts.awt.PointShapeFactory;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;

/**
 * A TileRenderer implementation which get all edges/vertex in the bounding box of the tile, and
 * call a GraphRenderer for getting rendering attributes of each (color, string label...).
 *
 * @author laurent
 */
public class TileRendererImpl implements TileRenderer {

    private GraphRenderer evRenderer;
    private TileRenderContext context;
    private Envelope bbox;
    private float lineWidth;
    private Collection<Vertex> vertices;
    private Set<Edge> edgesSet;
    private ShapeWriter shapeWriter = new ShapeWriter(new IdentityPointTransformation(),
            new PointShapeFactory.Point());
    private GeometryFactory geomFactory = new GeometryFactory();

    public TileRendererImpl(GraphRenderer evRenderer, TileRenderContext context) {
        this.evRenderer = evRenderer;
        this.context = context;
    }

    @Override
    public void renderTile() {
        initializeConfig();
        renderEdges();
        renderVertices();
    }

    private void initializeConfig() {
        setLineWidth();
        setMarginsToBbox();
        setVertices();
        setEdgesSet();
        addInComingOutgoingToVertices();
    }

    private void renderEdges() {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth));

        Stroke halfStroke = new BasicStroke(lineWidth * 0.6f + 1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL);
        Stroke halfDashedStroke = new BasicStroke(lineWidth * 0.6f + 1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 1.0f, new float[]{4 * lineWidth, lineWidth},
                2 * lineWidth);
        Stroke arrowStroke = new ShapeStroke(new Polygon(new int[]{0, 0, 30}, new int[]{0, 20,
                10}, 3), lineWidth / 2, 5.0f * lineWidth, 2.5f * lineWidth);
        BasicStroke thinStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL);

        BufferParameters bufParams = new BufferParameters();
        bufParams.setSingleSided(true);
        bufParams.setJoinStyle(BufferParameters.JOIN_BEVEL);

        EdgeVisualAttributes evAttrs = new EdgeVisualAttributes();
        for (Edge edge : edgesSet) {
            evAttrs.color = null;
            evAttrs.label = null;
            Geometry edgeGeom = edge.getGeometry();
            boolean hasGeom = true;
            if (edgeGeom == null) {
                Coordinate[] coordinates = new Coordinate[]{edge.getFromVertex().getCoordinate(),
                        edge.getToVertex().getCoordinate()};
                edgeGeom = GeometryUtils.getGeometryFactory().createLineString(coordinates);
                hasGeom = false;
            }

            boolean render = evRenderer.renderEdge(edge, evAttrs);
            if (!render)
                continue;

            Geometry midLineGeom = context.transform.transform(edgeGeom);
            OffsetCurveBuilder offsetBuilder = new OffsetCurveBuilder(new PrecisionModel(),
                    bufParams);
            Coordinate[] coords = offsetBuilder.getOffsetCurve(midLineGeom.getCoordinates(),
                    lineWidth * 0.4);
            if (coords.length < 2)
                continue; // Can happen for very small edges (<1mm)
            LineString offsetLine = geomFactory.createLineString(coords);
            Shape midLineShape = shapeWriter.toShape(midLineGeom);
            Shape offsetShape = shapeWriter.toShape(offsetLine);

            context.graphics.setStroke(hasGeom ? halfStroke : halfDashedStroke);
            context.graphics.setColor(evAttrs.color);
            context.graphics.draw(offsetShape);
            if (lineWidth > 6.0f) {
                context.graphics.setColor(Color.WHITE);
                context.graphics.setStroke(arrowStroke);
                context.graphics.draw(offsetShape);
            }
            if (lineWidth > 4.0f) {
                context.graphics.setColor(Color.BLACK);
                context.graphics.setStroke(thinStroke);
                context.graphics.draw(midLineShape);
            }
            if (evAttrs.label != null && lineWidth > 8.0f) {
                context.graphics.setColor(Color.BLACK);
                context.graphics.setStroke(new TextStroke("    " + evAttrs.label
                        + "                              ", font, false, true));
                context.graphics.draw(offsetShape);
            }
        }
    }

    private void renderVertices() {
        Font largeFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth * 1.5f));
        FontMetrics largeFontMetrics = context.graphics.getFontMetrics(largeFont);

        context.graphics.setFont(largeFont);
        context.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        context.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Stroke stroke = new BasicStroke(lineWidth * 1.4f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL);

        VertexVisualAttributes vvAttrs = new VertexVisualAttributes();
        for (Vertex vertex : vertices) {
            vvAttrs.color = null;
            vvAttrs.label = null;
            Point point = geomFactory.createPoint(new Coordinate(vertex.getLon(), vertex.getLat()));
            boolean render = evRenderer.renderVertex(vertex, vvAttrs);
            if (!render)
                continue;

            Point tilePoint = (Point) context.transform.transform(point);
            Shape shape = shapeWriter.toShape(tilePoint);

            context.graphics.setColor(vvAttrs.color);
            context.graphics.setStroke(stroke);
            context.graphics.draw(shape);
            if (vvAttrs.label != null && lineWidth > 6.0f
                    && context.bbox.contains(point.getCoordinate())) {
                context.graphics.setColor(Color.BLACK);
                int labelWidth = largeFontMetrics.stringWidth(vvAttrs.label);
                /*
                 * Poor man's solution: stay on the tile if possible. Otherwise the renderer would
                 * need to expand the envelope by an unbounded amount (max label size).
                 */
                double x = tilePoint.getX();
                if (x + labelWidth > context.tileWidth)
                    x -= labelWidth;
                context.graphics.drawString(vvAttrs.label, (float) x, (float) tilePoint.getY());
            }
        }
    }

    private void setLineWidth() {
        lineWidth = (float) (1.0f + 3.0f / Math.sqrt(context.metersPerPixel));
    }

    private void setMarginsToBbox() {
        bbox = context.expandPixels(lineWidth * 2.0, lineWidth * 2.0);
    }

    private void setVertices() {
        vertices = context.graph.streetIndex.getVerticesForEnvelope(bbox);
    }

    private void setEdgesSet() {
        Collection<Edge> edges = context.graph.streetIndex.getEdgesForEnvelope(bbox);
        edgesSet = new HashSet<>(edges);
    }

    private void addInComingOutgoingToVertices() {
        for (Vertex vertex : vertices) {
            edgesSet.addAll(vertex.getIncoming());
            edgesSet.addAll(vertex.getOutgoing());
        }
    }

    @Override
    public String getName() {
        return evRenderer.getName();
    }

    @Override
    public int getColorModel() {
        return BufferedImage.TYPE_INT_ARGB;
    }
}