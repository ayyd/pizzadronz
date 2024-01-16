package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import java.util.*;

/**
 * Finds a valid route. Implements a greedy best-first search algorithm.
 */
public class RouteFinder {
    private LngLat startPos;
    private LngLat destination;
    private PriorityQueue<Node> unvisited;
    private ArrayList<Node> visited;
    private LngLatHandler lngLatHandler;
    public ArrayList<Float> anglesOfRoute;

    public RouteFinder(LngLat startPos, LngLat destination) {
        this.startPos = startPos;
        this.destination = destination;
        unvisited = new PriorityQueue<>();
        visited = new ArrayList<>();
        lngLatHandler = new LngLatHandler();
        anglesOfRoute = new ArrayList<>();
    }

    public ArrayList<Node> getRoute() {
        unvisited.add(new Node(startPos, 999, startPos, getHeuristic(startPos)));
        while (!unvisited.isEmpty() && !lngLatHandler.isCloseTo(unvisited.peek().lngLat, destination)) {
            Node current = unvisited.poll();
            visited.add(current);
            assert current != null;
            for (Node neighbor : getNeighborNodes(current.lngLat)) {
                double costOfNeighbor = current.cost + getPathWeight(neighbor.lngLat) +
                        getDeltaHeuristic(current.lngLat, neighbor.lngLat);
                if (unvisited.contains(neighbor) && costOfNeighbor < getNeighborCostInQueue(neighbor)) {
                    if (!unvisited.remove(neighbor)) {     // should always be false if implementation is correct
                        System.err.println("Failed to remove node in the priority queue."); // this code should never be reached
                    }
                    unvisited.add(new Node(current.lngLat, neighbor.angle, neighbor.lngLat, costOfNeighbor));
                } else if (!unvisited.contains(neighbor) && !visited.contains(neighbor)) {
                    unvisited.add(new Node(current.lngLat, neighbor.angle, neighbor.lngLat, costOfNeighbor));
                }
            }
        }
        if (unvisited.isEmpty()) {
            System.err.println("No paths found.");      // if the algorithm failed to find a route, which should never happen
        }
        visited.add(unvisited.poll());
        return constructPathAsNode();
    }

    private ArrayList<Node> getNeighborNodes(LngLat node) {
        ArrayList<Node> neighborNodes = new ArrayList<>();
        float angle = 0;
        while (angle < 360) {
            neighborNodes.add(new Node(null, angle, lngLatHandler.nextPosition(node, angle), 0));
            angle += 22.5F;
        }
        return neighborNodes;
    }

    private double getNeighborCostInQueue(Node neighborNode) {
        for (Node n : unvisited) {
            if (n.equals(neighborNode)) {
                return n.cost;
            }
        }
        System.err.println("Failed to retrieve neighbor node cost in the priority queue.");
        throw new NoSuchElementException();     // this code should never be reached if implementation is correct
    }

    private double getPathWeight(LngLat position) {
        if (lngLatHandler.isInNoFlyZones(position)) {
            return Double.MAX_VALUE;
        }
        /*
         * A path weight to a flyable position must not dwarf the heuristic
         * so that the DRONE_MOVE_DISTANCE, 1.5E-4, must be divided by 20F or greater.
         * This ensures a greedy best-best first search.
         */
        return SystemConstants.DRONE_MOVE_DISTANCE / 20F;
    }

    private double getHeuristic(LngLat node) {
        return lngLatHandler.distanceTo(node, destination);
    }

    private double getDeltaHeuristic(LngLat currentPos, LngLat nextPos) {
        return getHeuristic(nextPos) - getHeuristic(currentPos);
    }

    /**
     * Return the path from the starting position to the destination.
     * @return
     */
    private ArrayList<Node> constructPathAsNode() {
        ArrayList<Node> paths = new ArrayList<>();
        Node current = visited.get(visited.size() - 1);
        paths.add(current);
        while (!current.equals(new Node(null, 0, startPos, 0))) {
            for (Node node : visited) {
                if (current.parentLnglat.equals(node.lngLat)) {
                    paths.add(0, node);
                    current = node;
                    break;
                }
            }
        }
        return paths;
    }

}
