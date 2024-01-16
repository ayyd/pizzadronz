package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

public record Edge(LngLat higherEnd, LngLat lowerEnd, double slope) {
}
