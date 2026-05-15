package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.ProcurementDto.*;
import com.weekendbasket.app.event.CycleClosedEvent;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.model.*;
import com.weekendbasket.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private static final Logger log = LogManager.getLogger(ProcurementService.class);

    private final ProcurementSheetRepository procurementRepository;
    private final WeeklyCycleRepository cycleRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final TransportTrackingService transportTrackingService;

    // Async — cycle close API returns immediately, aggregation runs on background thread
    @EventListener
    @Async("appTaskExecutor")
    @Transactional
    public void onCycleClosed(CycleClosedEvent event) {
        aggregateForCycle(event.cycleId());
    }

    // Called automatically when cycle closes
    @Transactional
    public void aggregateForCycle(Long cycleId) {
        if (procurementRepository.existsByCycleId(cycleId)) {
            log.info("Procurement already aggregated for cycle {}", cycleId);
            return;
        }
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));

        List<Object[]> rows = orderItemRepository.aggregateByProduct(cycleId);
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            BigDecimal qty = (BigDecimal) row[1];
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            procurementRepository.save(ProcurementSheet.builder()
                    .cycle(cycle)
                    .product(product)
                    .totalQuantity(qty)
                    .unit(product.getUnit())
                    .build());
        }
        log.info("Procurement aggregated for cycle {}: {} products", cycleId, rows.size());
        // Auto-trigger transport stage: PROCUREMENT_STARTED
        transportTrackingService.autoAddStage(cycleId, "PROCUREMENT_STARTED",
                "Procurement started automatically after cycle closed");
    }

    @Transactional(readOnly = true)
    public ProcurementSheetResponse getForCycle(Long cycleId) {
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));
        List<ProcurementItemResponse> items = procurementRepository.findByCycleId(cycleId)
                .stream().map(this::toItemResponse).toList();
        long totalOrders = orderRepository.countByCycleId(cycleId);
        return new ProcurementSheetResponse(cycleId, cycle.getCycleLabel(), totalOrders, items);
    }

    @Transactional
    public ProcurementItemResponse updateItem(Long itemId, UpdateProcurementRequest request) {
        ProcurementSheet sheet = procurementRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Procurement item not found: " + itemId));
        if (request.vendorName() != null) sheet.setVendorName(request.vendorName());
        if (request.vendorNotes() != null) sheet.setVendorNotes(request.vendorNotes());
        if (request.procuredQty() != null) sheet.setProcuredQty(request.procuredQty());
        if (request.status() != null) sheet.setStatus(request.status().toUpperCase());
        procurementRepository.save(sheet);

        // Check if ALL items in this cycle are now PROCURED → auto-trigger GOODS_LOADED
        Long cycleId = sheet.getCycle().getId();
        long totalItems   = procurementRepository.countByCycleId(cycleId);
        long procuredItems = procurementRepository.countByCycleIdAndStatus(cycleId, "PROCURED");
        if (totalItems > 0 && totalItems == procuredItems) {
            transportTrackingService.autoAddStage(cycleId, "GOODS_LOADED",
                    "All items procured — goods loaded automatically");
            log.info("All {} items procured for cycle {} — GOODS_LOADED triggered", totalItems, cycleId);
        }
        return toItemResponse(sheet);
    }

    // Mark ALL items in a cycle as PROCURED at once (bulk action)
    @Transactional
    public void markAllProcured(Long cycleId) {
        List<ProcurementSheet> items = procurementRepository.findByCycleId(cycleId);
        items.forEach(item -> {
            item.setStatus("PROCURED");
            procurementRepository.save(item);
        });
        // Auto-trigger GOODS_LOADED
        transportTrackingService.autoAddStage(cycleId, "GOODS_LOADED",
                "All items marked as procured — goods loaded");
        log.info("All {} items marked PROCURED for cycle {} — GOODS_LOADED triggered", items.size(), cycleId);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long cycleId) {
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));
        List<ProcurementSheet> sheets = procurementRepository.findByCycleId(cycleId);
        List<CustomerOrder> orders = orderRepository.findByCycleIdAndStatusNot(cycleId, "CANCELLED");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            buildSummarySheet(wb, cycle, sheets);
            buildPackingSheet(wb, orders);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    // ── Excel helpers ─────────────────────────────────────────────────────────

    private void buildSummarySheet(XSSFWorkbook wb, WeeklyCycle cycle, List<ProcurementSheet> sheets) {
        Sheet sheet = wb.createSheet("Procurement Summary");
        CellStyle header = headerStyle(wb);

        Row h = sheet.createRow(0);
        String[] cols = {"Product", "Unit", "Total Qty Needed", "Vendor", "Procured Qty", "Status"};
        for (int i = 0; i < cols.length; i++) { Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header); }

        int r = 1;
        for (ProcurementSheet ps : sheets) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(ps.getProduct().getName());
            row.createCell(1).setCellValue(ps.getUnit() != null ? ps.getUnit() : "");
            row.createCell(2).setCellValue(ps.getTotalQuantity().doubleValue());
            row.createCell(3).setCellValue(ps.getVendorName() != null ? ps.getVendorName() : "");
            row.createCell(4).setCellValue(ps.getProcuredQty() != null ? ps.getProcuredQty().doubleValue() : 0);
            row.createCell(5).setCellValue(ps.getStatus());
        }
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildPackingSheet(XSSFWorkbook wb, List<CustomerOrder> orders) {
        Sheet sheet = wb.createSheet("Customer Packing List");
        CellStyle header = headerStyle(wb);

        Row h = sheet.createRow(0);
        String[] cols = {"Order #", "Customer", "Flat", "Block", "Delivery Slot", "Product", "Qty", "Unit"};
        for (int i = 0; i < cols.length; i++) { Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header); }

        int r = 1;
        for (CustomerOrder order : orders) {
            UserProfile profile = userProfileRepository.findByUserId(order.getUser().getId()).orElse(null);
            String name = profile != null ? profile.getFirstName() + " " + profile.getLastName() : order.getUser().getUsername();
            String flat = profile != null ? profile.getFlatNumber() : "";
            String block = profile != null ? profile.getBlock() : "";

            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(name);
                row.createCell(2).setCellValue(flat != null ? flat : "");
                row.createCell(3).setCellValue(block != null ? block : "");
                row.createCell(4).setCellValue(order.getDeliverySlot() != null ? order.getDeliverySlot() : "");
                row.createCell(5).setCellValue(item.getProduct().getName());
                row.createCell(6).setCellValue(item.getQuantity().doubleValue());
                row.createCell(7).setCellValue(item.getProduct().getUnit() != null ? item.getProduct().getUnit() : "");
            }
        }
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private ProcurementItemResponse toItemResponse(ProcurementSheet ps) {
        return new ProcurementItemResponse(
                ps.getId(), ps.getProduct().getId(), ps.getProduct().getName(),
                ps.getUnit(), ps.getTotalQuantity(), ps.getVendorName(),
                ps.getVendorNotes(), ps.getProcuredQty(), ps.getStatus());
    }
}
