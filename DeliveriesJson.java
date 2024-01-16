package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;

public record DeliveriesJson(String orderNo, OrderStatus orderStatus, OrderValidationCode orderValidationCode, int costInPence) {
}
