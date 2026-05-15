package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_table",
        uniqueConstraints = @UniqueConstraint(columnNames = {"type", "lookup_code"}, name = "uq_master_type_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterTable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lookup_value", length = 10)
    private String lookupValue;

    @Column(name = "lookup_item", nullable = false, length = 100)
    private String lookupItem;

    @Column(name = "lookup_code", nullable = false, length = 50)
    private String lookupCode;

    @Column(name = "type", nullable = false, length = 50)
    private String type;
}
