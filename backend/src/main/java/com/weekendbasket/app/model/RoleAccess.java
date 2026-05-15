package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_access", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "role_id"}, name = "uq_role_access_user_role")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAccess extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
