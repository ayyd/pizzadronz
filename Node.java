package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

public class Node implements Comparable<Node>{

    public LngLat parentLnglat;
    public float angle;
    public LngLat lngLat;
    public double cost;

    public Node(LngLat parentLnglat, float angle, LngLat lngLat, double cost) {
        this.parentLnglat = parentLnglat;
        this.angle = angle;
        this.lngLat = lngLat;
        this.cost = cost;
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     *
     *
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @param n the SingleMove object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     * @apiNote It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     */
    @Override
    public int compareTo(Node n) {
        if (cost == n.cost) {
            return 0;
        }
        return (cost < n.cost) ? -1 : 1;
    }

    /**
     * Used by contains method.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node)) {
            return false;
        }
        Node n = (Node) o;
        return n.lngLat.equals(this.lngLat);
    }
}
