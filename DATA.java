package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.*;

import java.util.ArrayList;

public final class DATA {
    public final static String RESTAURANTS_URL = "restaurants";
    public final static String NO_FLY_ZONES_URL = "noFlyZones";
    public final static String CENTRAL_AREA_URL = "centralArea";
    public final static String ORDERS_URL = "orders/";
    public final static LngLat APPLETON_TOWER = new LngLat(-3.186874, 55.944494);
    public static NamedRegion CENTRAL_REGION;
    public static ArrayList<NoFlyZone> NO_FLY_ZONES;
    public static Restaurant[] RESTAURANTS;
    public static Order[] ORDERS;
}
