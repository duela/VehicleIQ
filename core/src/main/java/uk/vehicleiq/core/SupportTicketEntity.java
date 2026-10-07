package uk.vehicleiq.core;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="support_tickets", indexes=@Index(name="idx_ticket_tenant", columnList="tenantId"))
class SupportTicketEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false) private String tenantId;
    @Column(nullable=false) private String requester;
    @Column(nullable=false) private String subject;
    @Lob private String message;
    @Column(nullable=false) private String status="OPEN";
    @Column(nullable=false) private String priority="NORMAL";
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected SupportTicketEntity() {}
    SupportTicketEntity(String tenant,String name,String title,String body){tenantId=tenant;requester=name;subject=title;message=body;}
    String getId(){return id;} String getTenantId(){return tenantId;} String getRequester(){return requester;} String getSubject(){return subject;}
    String getMessage(){return message;} String getStatus(){return status;} void setStatus(String v){status=v;} String getPriority(){return priority;} void setPriority(String v){priority=v;} Instant getCreatedAt(){return createdAt;}
}

