package com.weekendbasket.app.service;

import com.weekendbasket.app.model.CustomerOrder;
import com.weekendbasket.app.model.OrderItem;
import com.weekendbasket.app.model.ProcurementSheet;
import com.weekendbasket.app.model.UserProfile;
import com.weekendbasket.app.model.WeeklyCycle;
import com.weekendbasket.app.repository.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcurementEmailService {

    private static final Logger log = LogManager.getLogger(ProcurementEmailService.class);

    private final JavaMailSender mailSender;
    private final RoleAccessRepository roleAccessRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProcurementSheetRepository procurementSheetRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WeeklyCycleRepository cycleRepository;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public void sendProcurementExcel(Long cycleId, String cycleLabel, long totalOrders) {
        if (!mailEnabled) {
            log.info("Mail disabled — skipping procurement email for cycle {}", cycleId);
            return;
        }

        List<String> recipients = roleAccessRepository.findByRoleId("ROLE_PROCUREMENT").stream()
                .map(ra -> userProfileRepository.findByUserId(ra.getUser().getId())
                        .map(UserProfile::getEmail).orElse(null))
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();

        if (recipients.isEmpty()) {
            log.info("No PROCUREMENT users with email — skipping email for cycle {}", cycleId);
            return;
        }

        byte[] excelBytes = buildExcel(cycleId, cycleLabel);

        for (String email : recipients) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setTo(email);
                helper.setSubject("FarmMitra — Procurement Sheet: " + cycleLabel);
                helper.setText("Hi,\n\nPlease find this week's procurement sheet attached.\n\n"
                        + "Total orders: " + totalOrders + "\n"
                        + "Cycle: " + cycleLabel + "\n\n"
                        + "— FarmMitra Ops");
                helper.addAttachment("procurement-" + cycleId + ".xlsx",
                        new ByteArrayResource(excelBytes),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                mailSender.send(message);
                log.info("Procurement email sent to {} for cycle {}", email, cycleId);
            } catch (MessagingException e) {
                log.error("Failed to send procurement email to {} for cycle {}: {}", email, cycleId, e.getMessage());
            }
        }
    }

    private byte[] buildExcel(Long cycleId, String cycleLabel) {
        WeeklyCycle cycle = cycleRepository.findById(cycleId).orElseThrow();
        List<ProcurementSheet> sheets = procurementSheetRepository.findByCycleId(cycleId);
        List<CustomerOrder> orders = orderRepository.findByCycleIdAndStatusNot(cycleId, "CANCELLED");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(wb);

            // Sheet 1 — Procurement Summary
            Sheet summary = wb.createSheet("Procurement Summary");
            String[] sumCols = {"Product", "Unit", "Total Qty Needed", "Vendor", "Procured Qty", "Status"};
            writeHeader(summary, sumCols, headerStyle);
            int r = 1;
            for (ProcurementSheet ps : sheets) {
                Row row = summary.createRow(r++);
                row.createCell(0).setCellValue(ps.getProduct().getName());
                row.createCell(1).setCellValue(ps.getUnit() != null ? ps.getUnit() : "");
                row.createCell(2).setCellValue(ps.getTotalQuantity().doubleValue());
                row.createCell(3).setCellValue(ps.getVendorName() != null ? ps.getVendorName() : "");
                row.createCell(4).setCellValue(ps.getProcuredQty() != null ? ps.getProcuredQty().doubleValue() : 0);
                row.createCell(5).setCellValue(ps.getStatus());
            }
            for (int i = 0; i < sumCols.length; i++) summary.autoSizeColumn(i);

            // Sheet 2 — Customer Packing List
            Sheet packing = wb.createSheet("Customer Packing List");
            String[] packCols = {"Order #", "Customer", "Flat", "Block", "Delivery Slot", "Product", "Qty", "Unit"};
            writeHeader(packing, packCols, headerStyle);
            r = 1;
            for (CustomerOrder order : orders) {
                UserProfile profile = userProfileRepository.findByUserId(order.getUser().getId()).orElse(null);
                String name = profile != null ? profile.getFirstName() + " " + profile.getLastName() : order.getUser().getUsername();
                for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                    Row row = packing.createRow(r++);
                    row.createCell(0).setCellValue(order.getOrderNumber());
                    row.createCell(1).setCellValue(name);
                    row.createCell(2).setCellValue(profile != null && profile.getFlatNumber() != null ? profile.getFlatNumber() : "");
                    row.createCell(3).setCellValue(profile != null && profile.getBlock() != null ? profile.getBlock() : "");
                    row.createCell(4).setCellValue(order.getDeliverySlot() != null ? order.getDeliverySlot() : "");
                    row.createCell(5).setCellValue(item.getProduct().getName());
                    row.createCell(6).setCellValue(item.getQuantity().doubleValue());
                    row.createCell(7).setCellValue(item.getProduct().getUnit() != null ? item.getProduct().getUnit() : "");
                }
            }
            for (int i = 0; i < packCols.length; i++) packing.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate procurement Excel for email", e);
        }
    }

    private void writeHeader(Sheet sheet, String[] cols, CellStyle style) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
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
}
