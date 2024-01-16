package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.*;
import com.mapbox.geojson.*;

import uk.ac.ed.inf.ilp.constant.*;
import uk.ac.ed.inf.ilp.data.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        // run-time passed-in arguments are a data, YYYY-MM-DD, and a base URL
        if (args.length != 2){
            System.err.println("Testclient Base-URL Echo-Parameter");
            System.err.println("you must supply the base address of the ILP REST Service\n" +
                    " e.g. http://restservice.somewhere and a string to be echoed");
            System.exit(1);
        }

        try {
            String baseURL = args[1];
            String orderDate = args[0];

            if (!baseURL.endsWith("/")) {
                baseURL += "/";
            }

            URL centralRegionURL = new URL(baseURL + DATA.CENTRAL_AREA_URL);
            URL noFlyZonesURL = new URL(baseURL + DATA.NO_FLY_ZONES_URL);
            URL restaurantsURL = new URL(baseURL + DATA.RESTAURANTS_URL);
            URL orderOfDateURL = new URL(baseURL + DATA.ORDERS_URL + orderDate);

            DATA.RESTAURANTS = new ObjectMapper().readValue(restaurantsURL, Restaurant[].class);
            DATA.ORDERS = new ObjectMapper().registerModule(new JSR310Module()).readValue(orderOfDateURL, Order[].class);
            DATA.CENTRAL_REGION = new ObjectMapper().readValue(centralRegionURL, NamedRegion.class);
            new LngLatHandler().initialiseNoFlyZones(new ObjectMapper().readValue(noFlyZonesURL, NamedRegion[].class));

        } catch (MalformedURLException e) {
            System.err.println("URL Invalid");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrderValidator orderValidator = new OrderValidator();
        int numberOfValidOrders = 0;
        ArrayList<Restaurant> restaurantsInValidOrder = new ArrayList<>();
        for (int i = 0; i < DATA.ORDERS.length; i++) {
            Order thisOrder = DATA.ORDERS[i];
            orderValidator.validateOrder(thisOrder, DATA.RESTAURANTS);
            if (!thisOrder.getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                continue;
            }
            numberOfValidOrders ++;
            if (!restaurantsInValidOrder.contains(getRestaurantOfAnOrder(thisOrder))) {
                restaurantsInValidOrder.add(getRestaurantOfAnOrder(thisOrder));
            }
            thisOrder.setOrderStatus(OrderStatus.DELIVERED);
        }

        Node[][] routesCollection = new Node[restaurantsInValidOrder.size()][];
        for (int i = 0; i < restaurantsInValidOrder.size(); i++) {
            RouteFinder router = new RouteFinder(DATA.APPLETON_TOWER, restaurantsInValidOrder.get(i).location());
            routesCollection[i] = getForthAndBackRoute(router.getRoute());
        }

        Node[][] paths = new Node[DATA.ORDERS.length][];
        for (int i = 0; i < DATA.ORDERS.length; i++) {
            Order thisOrder = DATA.ORDERS[i];
            if (!thisOrder.getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                continue;
            }
            paths[i] = getRouteForThisRestaurant(getRestaurantOfAnOrder(thisOrder), restaurantsInValidOrder, routesCollection);
        }

        List<Feature> listOfRoutes = new ArrayList<>();
        for(Node[] nodes : paths) {
            if (nodes != null) {
                ArrayList<Point> routeCoordinates = new ArrayList<>();
                for (Node node : nodes) {
                    routeCoordinates.add(Point.fromLngLat(node.lngLat.lng(), node.lngLat.lat()));
                }
                listOfRoutes.add(Feature.fromGeometry(LineString.fromLngLats(routeCoordinates)));
            }
        }
        FeatureCollection allRoutes = FeatureCollection.fromFeatures(listOfRoutes);

        File dir = new File("resultfiles");
        dir.mkdir();
        BufferedWriter geojsonWriter = new BufferedWriter(new FileWriter(new File(dir, "drone-" + args[0] + ".geojson")));
        geojsonWriter.write(allRoutes.toJson());
        geojsonWriter.close();

        DeliveriesJson[] deliveriesJsons = new DeliveriesJson[DATA.ORDERS.length];
        for (int i = 0; i < deliveriesJsons.length; i++) {
            Order order = DATA.ORDERS[i];
            deliveriesJsons[i] = new DeliveriesJson(order.getOrderNo(), order.getOrderStatus(),
                    order.getOrderValidationCode(), order.getPriceTotalInPence());
        }
        BufferedWriter deliveriesWriter = new BufferedWriter(new FileWriter(new File(dir, "deliveries-" + args[0] + ".json")));
        deliveriesWriter.write(new ObjectMapper().writer().writeValueAsString(deliveriesJsons));
        deliveriesWriter.close();

        FlightpathJson[][] flightpathJsons = new FlightpathJson[numberOfValidOrders][];
        int flightpathJsonsIndex = 0;
        for (int i = 0; i < DATA.ORDERS.length; i++) {
            if (DATA.ORDERS[i].getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                String orderNo = DATA.ORDERS[i].getOrderNo();
                flightpathJsons[flightpathJsonsIndex] = new FlightpathJson[paths[i].length];
                for (int j = 0; j < paths[i].length; j++) {
                    flightpathJsons[flightpathJsonsIndex][j] = new FlightpathJson(orderNo,
                            (float)paths[i][j].parentLnglat.lng(),
                            (float)paths[i][j].parentLnglat.lat(),
                            paths[i][j].angle,
                            (float)paths[i][j].lngLat.lng(),
                            (float)paths[i][j].lngLat.lat());
                }
                flightpathJsonsIndex++;
            }
        }
        BufferedWriter flightPathWriter = new BufferedWriter(new FileWriter(new File(dir, "flightpath-" + args[0] + ".json")));
        flightPathWriter.write(new ObjectMapper().writer().writeValueAsString(flightpathJsons));
        flightPathWriter.close();
    }

    private static Restaurant getRestaurantOfAnOrder(Order order) {
        for (Restaurant restaurant : DATA.RESTAURANTS) {
            for (Pizza menuPizza : restaurant.menu()) {
                if (order.getPizzasInOrder()[0].equals(menuPizza)) {
                    return restaurant;
                }
            }
        }
        System.err.println("No Restaurant has pizza in this order.");
        return null;    // should never be reached with correct implementation
    }

    private static Node[] getForthAndBackRoute(ArrayList<Node> forthPath) {
        // forth path does not contain 999 hover at the restaurant
        ArrayList<Node> path = new ArrayList<>(forthPath);
        LngLat destinationLngLat = forthPath.get(forthPath.size() - 1).lngLat;
        path.add(new Node(destinationLngLat, 999, destinationLngLat, 0));   // cost is irrelevant here
        for (int i = forthPath.size() - 1; i > 0; i--) {
            path.add(new Node(forthPath.get(i).lngLat, (forthPath.get(i).angle + 180F) % 360F,
                    forthPath.get(i).parentLnglat, 0));
        }
        path.add(forthPath.get(0));
        return path.toArray(new Node[1]);
    }

    private static Node[] getRouteForThisRestaurant(Restaurant thisRestaurant, ArrayList<Restaurant> restaurantsInValidOrder, Node[][] routesCollection) {
        int restaurantIndex = 0;
        for (int i = 0; i < restaurantsInValidOrder.size(); i++) {
            if (thisRestaurant.equals(restaurantsInValidOrder.get(i))) {
                restaurantIndex = i;
                break;
            }
        }
        return routesCollection[restaurantIndex];
    }


}
