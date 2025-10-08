package com.enterprise.km.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String resource; // e.g., "document", "user", "department"

    @Column(nullable = false)
    private String action; // e.g., "read", "write", "delete", "manage"
}
