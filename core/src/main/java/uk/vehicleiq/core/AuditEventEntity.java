package uk.vehicleiq.core;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="audit_events", indexes={@Index(name="idx_audit_tenant", columnList="tenantId"), @Index(name="idx_audit_time", columnList="occurredAt")})
class AuditEventEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false) private String tenantId;
    @Column(nullable=false) private String actor;
    @Column(nullable=false) private String action;
    private String resourceId;
    @Column(nullable=false) private Instant occurredAt=Instant.now();
    protected AuditEventEntity() {}
    AuditEventEntity(String tenant,String actor,String action,String resource){tenantId=tenant;this.actor=actor;this.action=action;resourceId=resource;}
    String getId(){return id;} String getTenantId(){return tenantId;} String getActor(){return actor;} String getAction(){return action;} String getResourceId(){return resourceId;} Instant getOccurredAt(){return occurredAt;}
}

