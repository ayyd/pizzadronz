package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.CentralRegionVertexOrder;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implements the interface LngLatHandling.
 */
public class LngLatHandler implements LngLatHandling {
    /**
     * Calculates the distance between two position using Euclidean distance formula.
     * @param startPosition the start position object
     * @param endPosition the end position object
     * @return the distance in double
     */
    @Override
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        return Math.sqrt( Math.pow((endPosition.lng() - startPosition.lng()), 2) +
                Math.pow((endPosition.lat() - startPosition.lat()), 2) );
    }

    /**
     * Checks if a position is in strict proximity of another. Proximity defined by a given system constant.
     * @param startPosition the start position object
     * @param otherPosition the end position object
     * @return if a position is in strict proximity of another
     */
    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) < SystemConstants.DRONE_IS_CLOSE_DISTANCE;  // strictly less than
    }

    /**
     * Calculates the next position of the move given an angle.
     * @param startPosition the start position object
     * @param angle the angle of the move
     * @return the destination position after the move
     */
    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        return new LngLat(Math.cos(Math.toRadians(angle)) * SystemConstants.DRONE_MOVE_DISTANCE + startPosition.lng(),
                Math.sin(Math.toRadians(angle)) * SystemConstants.DRONE_MOVE_DISTANCE + startPosition.lat());
    }

    /**
     * Checks if a position is within the central region: a closed rectangle.
     * NOTE: this method is only used for the central region, NOT the no-fly zones.
     * @param position the start position object
     * @param region a closed polygon object, currently a rectangle
     * @return if a position is inside a region
     */
    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        // the central region is a closed rectangular shape
        // include the edges
        return position.lng() >= region.vertices()[CentralRegionVertexOrder.TOP_LEFT].lng() &&
                position.lng() <= region.vertices()[CentralRegionVertexOrder.TOP_RIGHT].lng() &&
                position.lat() >= region.vertices()[CentralRegionVertexOrder.BOTTOM_LEFT].lat() &&
                position.lat() <= region.vertices()[CentralRegionVertexOrder.TOP_LEFT].lat();
    }

    public boolean isInCentralRegion(LngLat pos) {
        return isInRegion(pos, DATA.CENTRAL_REGION);
    }

    public void initialiseNoFlyZones(NamedRegion[] noFlyZones) {
        ArrayList<NoFlyZone> initialisedZones = new ArrayList<>();
        for (NamedRegion noFlyZone : noFlyZones) {
            initialisedZones.add(new NoFlyZone(findEdge(noFlyZone), noFlyZone.vertices()));
        }
        DATA.NO_FLY_ZONES = initialisedZones;
    }

    private Edge[] findEdge(NamedRegion region) {
        Edge[] edges = new Edge[region.vertices().length - 1];
        for (int i = 0; i < region.vertices().length - 1; i++) {
            LngLat[] orderedVertices = getVertexInorder(region.vertices()[i], region.vertices()[i + 1]);
            edges[i] = new Edge(orderedVertices[0], orderedVertices[1],
                    getSlopeOfALine(orderedVertices[0], orderedVertices[1]));
        }
        return edges;
    }
    // lat -y
    // long - x
    private LngLat[] getVertexInorder(LngLat firstEnd, LngLat secondEnd) {
        LngLat[] vertices = new LngLat[2];
        if (firstEnd.lat() >= secondEnd.lat()) {
            vertices[0] = firstEnd;      // index 0 vertex has a higher or equivalent latitude
            vertices[1] = secondEnd;     // index 1 vertex has a lower latitude
        } else {
            vertices[0] = secondEnd;
            vertices[1] = firstEnd;
        }
        return vertices;
    }

    private double getSlopeOfALine(LngLat firstVertex, LngLat secondVertex) {
        double slope = 0;
        try {
            slope = (firstVertex.lat() - secondVertex.lat()) / (firstVertex.lng() - secondVertex.lng());
        } catch (ArithmeticException e) {
            // divider = 0, i.e., a vertical edge
            slope = Double.MAX_VALUE;   // a vertical edge has an infinite slope
        }
        return slope;
    }

    public boolean isInNoFlyZones(LngLat pos) {
        for (NoFlyZone zone : DATA.NO_FLY_ZONES) {
            if ((findNumberOfCrossingEdges(pos, zone) % 2) != 0) {      // is odd
                return true;
            }
//             to solve the case of a path cutting the corner of a no-fly zone
            for (LngLat noFlyZoneVertex : zone.vertices()) {
                if (distanceTo(pos, noFlyZoneVertex) < SystemConstants.DRONE_MOVE_DISTANCE * 0.8) { // should be 0.8; no greater than 0.8
                    return true;
                }
            }
        }
        return false;
    }

    // lat -y
    // long - x

    private int findNumberOfCrossingEdges(LngLat pos, NoFlyZone zone) {
        int numberOfCrosses = 0;
        for (Edge edge : zone.edges()) {

            // a horizontal edge and the position has the same latitude as the edge
            // we only consider when the position lies on the edge
            if (pos.lat() == edge.lowerEnd().lat() && pos.lat() == edge.higherEnd().lat() &&
                    pos.lng() >= Math.min(edge.higherEnd().lng(), edge.lowerEnd().lng()) &&
                    pos.lng() <= Math.max(edge.higherEnd().lng(), edge.lowerEnd().lng())) {
                numberOfCrosses += 1;
            }

            // no >= or <= as a position on the left of a horizontal edge will be considered as crossing the edge
            else if (pos.lat() > edge.lowerEnd().lat() && pos.lat() < edge.higherEnd().lat() &&
                    pos.lng() <= Math.max(edge.higherEnd().lng(), edge.lowerEnd().lng())) {      // casting to the right

                double slopeOfLineFromPosToLowerEndOfEdge = getSlopeOfALine(edge.lowerEnd(), pos);

                if (edge.slope() == Double.MAX_VALUE && slopeOfLineFromPosToLowerEndOfEdge != Double.MAX_VALUE) {
                    // the edge is vertical but the line is not
                    if (slopeOfLineFromPosToLowerEndOfEdge < 0) {   // the position is on the left of the edge
                        numberOfCrosses += 1;
                    }
                }
                else if (edge.slope() != Double.MAX_VALUE && slopeOfLineFromPosToLowerEndOfEdge == Double.MAX_VALUE) {
                    // the edge is not vertical, but the line from the position to the lower end of the edge is
                    if (edge.slope() > 0) {
                        numberOfCrosses += 1;
                    }
                    // edge.slope() < 0, do nothing
                } else if (edge.slope() == Double.MAX_VALUE && slopeOfLineFromPosToLowerEndOfEdge == Double.MAX_VALUE) {
                    // both edge and the line are vertical, i.e., the position lies on the edge
                    numberOfCrosses += 1;
                } else if(slopeOfLineFromPosToLowerEndOfEdge >= edge.slope()){
                    // when both edge and the line are not vertical
                    // when crosses the edge or lies on it
                        numberOfCrosses += 1;
                } else if (slopeOfLineFromPosToLowerEndOfEdge < 0 && edge.slope() > 0) {
                    numberOfCrosses += 1;
                }
            }

        }
        return numberOfCrosses;
    }

}
