package com.weekendbasket.app.config;

import com.weekendbasket.app.model.MasterTable;
import com.weekendbasket.app.model.Role;
import com.weekendbasket.app.repository.MasterTableRepository;
import com.weekendbasket.app.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LogManager.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final MasterTableRepository masterTableRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedMasterTable();
    }

    private void seedRoles() {
        List<Object[]> roles = List.of(
                new Object[]{"Super Admin", "ROLE_SUPER_ADMIN"},
                new Object[]{"Admin",       "ROLE_ADMIN"},
                new Object[]{"Customer",    "ROLE_CUSTOMER"},
                new Object[]{"Delivery",    "ROLE_DELIVERY"}
        );
        for (Object[] r : roles) {
            String roleId = (String) r[1];
            if (!roleRepository.existsByRoleId(roleId)) {
                roleRepository.save(Role.builder().roleName((String) r[0]).roleId(roleId).build());
                log.info("Seeded role: {}", roleId);
            }
        }
    }

    private void seedMasterTable() {
        // type, lookupCode, lookupItem, lookupValue
        List<Object[]> entries = List.of(
                new Object[]{"ORDER_STATUS",      "PLACED",                 "Placed",                "1"},
                new Object[]{"ORDER_STATUS",      "CONFIRMED",              "Confirmed",             "2"},
                new Object[]{"ORDER_STATUS",      "PACKED",                 "Packed",                "3"},
                new Object[]{"ORDER_STATUS",      "DELIVERED",              "Delivered",             "4"},
                new Object[]{"ORDER_STATUS",      "CANCELLED",              "Cancelled",             "5"},
                new Object[]{"PAYMENT_METHOD",    "COD",                    "Cash on Delivery",      "1"},
                new Object[]{"PAYMENT_STATUS",    "PENDING",                "Pending",               "1"},
                new Object[]{"PAYMENT_STATUS",    "PAID",                   "Paid",                  "2"},
                new Object[]{"DELIVERY_SLOT",     "SAT",                    "Saturday",              "1"},
                new Object[]{"DELIVERY_SLOT",     "SUN",                    "Sunday",                "2"},
                new Object[]{"CYCLE_STATUS",      "OPEN",                   "Open",                  "1"},
                new Object[]{"CYCLE_STATUS",      "CLOSED",                 "Closed",                "2"},
                new Object[]{"CYCLE_STATUS",      "PROCUREMENT",            "Procurement",           "3"},
                new Object[]{"CYCLE_STATUS",      "DELIVERING",             "Delivering",            "4"},
                new Object[]{"CYCLE_STATUS",      "COMPLETED",              "Completed",             "5"},
                new Object[]{"TRANSPORT_STAGE",   "PROCUREMENT_STARTED",    "Procurement Started",   "1"},
                new Object[]{"TRANSPORT_STAGE",   "GOODS_LOADED",           "Goods Loaded",          "2"},
                new Object[]{"TRANSPORT_STAGE",   "IN_TRANSIT",             "In Transit",            "3"},
                new Object[]{"TRANSPORT_STAGE",   "ARRIVED",                "Arrived",               "4"},
                new Object[]{"TRANSPORT_STAGE",   "PACKING",                "Packing",               "5"},
                new Object[]{"TRANSPORT_STAGE",   "DISPATCHED",             "Dispatched",            "6"},
                new Object[]{"NOTIFICATION_TYPE", "ORDER_UPDATE",           "Order Update",          "1"},
                new Object[]{"NOTIFICATION_TYPE", "ANNOUNCEMENT",           "Announcement",          "2"},
                new Object[]{"NOTIFICATION_TYPE", "OFFER",                  "Offer",                 "3"},
                new Object[]{"NOTIFICATION_TYPE", "REMINDER",               "Reminder",              "4"}
        );
        for (Object[] e : entries) {
            String type = (String) e[0];
            String code = (String) e[1];
            if (!masterTableRepository.existsByTypeAndLookupCode(type, code)) {
                masterTableRepository.save(MasterTable.builder()
                        .type(type).lookupCode(code)
                        .lookupItem((String) e[2]).lookupValue((String) e[3])
                        .build());
                log.info("Seeded master: {}/{}", type, code);
            }
        }
    }
}
