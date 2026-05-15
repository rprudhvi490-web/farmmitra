package com.weekendbasket.app.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProcurementDto {

    public record ProcurementItemResponse(
            Long id,
            Long productId,
            String productName,
            String unit,
            BigDecimal totalQuantity,
            String vendorName,
            String vendorNotes,
            BigDecimal procuredQty,
            String status
    ) {}

    public record ProcurementSheetResponse(
            Long cycleId,
            String cycleLabel,
            long totalOrders,
            List<ProcurementItemResponse> items
    ) {}

    public record UpdateProcurementRequest(
            String vendorName,
            String vendorNotes,
            BigDecimal procuredQty,
            String status
    ) {}
}
