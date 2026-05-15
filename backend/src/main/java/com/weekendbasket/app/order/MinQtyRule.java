package com.weekendbasket.app.order;

import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Product;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(4)
public class MinQtyRule implements OrderPlacementRule {

    @Override
    public void validate(OrderPlacementContext context) {
        context.getRequest().items().forEach(item -> {
            Product product = context.getResolvedProducts().get(item.productId());
            BigDecimal minQty = product.getMinOrderQty();
            if (minQty != null && item.quantity().compareTo(minQty) < 0) {
                throw new WeekendBasketException(
                        String.format("Minimum order quantity for %s is %s %s",
                                product.getName(), minQty, product.getUnit()));
            }
        });
    }
}
