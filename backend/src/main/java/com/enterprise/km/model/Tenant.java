package com.enterprise.km.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column
    private String settings; // JSON string for tenant-specific settings
}
