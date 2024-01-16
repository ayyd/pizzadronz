package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.*;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.ilp.interfaces.*;

import java.time.*;
import java.util.*;


/**
 * Implements the interface of OrderValidation.
 */
public class OrderValidator implements OrderValidation {

    /**
     * Validates an order by checking all its parameters.
     * @param orderToValidate the order to be validated
     * @param definedRestaurants an array of Restaurant objects
     * @return a validated order with set OrderStatus and OrderValidationCode.
     */
    @Override
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {

        // pizza not defined or below the minimum of one pizza or any of the pizza in order is null
        if (orderToValidate.getPizzasInOrder() == null || orderToValidate.getPizzasInOrder().length == 0 ||
                Arrays.asList(orderToValidate.getPizzasInOrder()).contains(null)) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
            return orderToValidate;
        }

        // check if every ordered pizza is defined in any menu of any restaurant
        for (Pizza orderedpizza : orderToValidate.getPizzasInOrder()) {
            boolean isPizzaDefined = false;
            for (Restaurant restaurant : definedRestaurants) {
                if (Arrays.stream(restaurant.menu()).toList().contains(orderedpizza)) {
                    isPizzaDefined = true;
                }
            }
            if (!isPizzaDefined) {
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                return orderToValidate;
            }
        }


        // exceed the maximum number of pizzas
        if (orderToValidate.getPizzasInOrder().length > SystemConstants.MAX_PIZZAS_PER_ORDER) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            return orderToValidate;
        }


        // credit card number invalid
        if (orderToValidate.getCreditCardInformation().getCreditCardNumber() == null ||
                orderToValidate.getCreditCardInformation().getCreditCardNumber().length() != 16 ||
                !orderToValidate.getCreditCardInformation().getCreditCardNumber().matches("\\d+")) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            return orderToValidate;
        }


        // credit card expiry date invalid
        if (orderToValidate.getCreditCardInformation().getCreditCardExpiry() == null ||
                orderToValidate.getCreditCardInformation().getCreditCardExpiry().length() != 5) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        // credit card expiry date invalid
        try {
            int month = Integer.parseInt(orderToValidate.getCreditCardInformation().getCreditCardExpiry().substring(0, 2));
            int year = Integer.parseInt(orderToValidate.getCreditCardInformation().getCreditCardExpiry().substring(3));
            char slash = orderToValidate.getCreditCardInformation().getCreditCardExpiry().charAt(2);
            if (slash != '/' || month < 1 || month > 12 || year < LocalDate.now().getYear() - 2000 ||
                    (month < LocalDate.now().getMonthValue() &&
                    year == LocalDate.now().getYear() - 2000)) {
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
                return orderToValidate;
            }
        } catch (NumberFormatException e) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }


        // credit card cvv invalid
        if (orderToValidate.getCreditCardInformation().getCvv() == null ||
                orderToValidate.getCreditCardInformation().getCvv().length() != 3 ||
                !orderToValidate.getCreditCardInformation().getCvv().matches("\\d+")) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            return orderToValidate;
        }


        // check if ordering pizzas from multiple restaurants
        // for every restaurant, check the ordered pizza(s) against its menu
        Restaurant orderedRestaurant = null;
        for (Restaurant restaurant : definedRestaurants) {
            boolean isAPizzaInThisRestaurant = false;
            boolean isAPizzaNotInThisRestaurant = false;
            List<Pizza> menuList = Arrays.stream(restaurant.menu()).toList();
            for (Pizza pizza : orderToValidate.getPizzasInOrder()) {
                if (menuList.contains(pizza)) {
                    isAPizzaInThisRestaurant = true;
                    orderedRestaurant = restaurant;
                    continue;
                }
                isAPizzaNotInThisRestaurant = true;
            }
            if (isAPizzaInThisRestaurant && isAPizzaNotInThisRestaurant) {
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
                return orderToValidate;
            }
        }


        // check if restaurant is closed
        if (!Arrays.asList(orderedRestaurant.openingDays()).contains(orderToValidate.getOrderDate().getDayOfWeek())) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            return orderToValidate;
        }


        // check if total amount incorrect
        int priceTotalInPence = 0;
        for (Pizza pizza : orderToValidate.getPizzasInOrder()) {
            priceTotalInPence += pizza.priceInPence();
        }
        if (orderToValidate.getPriceTotalInPence() != priceTotalInPence + SystemConstants.ORDER_CHARGE_IN_PENCE) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            return orderToValidate;
        }

        // if there's no problem with the order, then validate it
        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        return orderToValidate;
    }


}
