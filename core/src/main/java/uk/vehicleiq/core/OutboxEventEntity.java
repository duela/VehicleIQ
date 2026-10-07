package uk.vehicleiq.core;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="outbox_events", indexes=@Index(name="idx_outbox_pending", columnList="status"))
class OutboxEventEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false) private String type;
    @Column(nullable=false) private String aggregateId;
    @Lob private String payload;
    @Column(nullable=false) private String status="PENDING";
    @Column(nullable=false) private Instant occurredAt=Instant.now();
    protected OutboxEventEntity() {}
    OutboxEventEntity(String type,String aggregate,String payload){this.type=type;aggregateId=aggregate;this.payload=payload;}
    String getId(){return id;} String getType(){return type;} String getAggregateId(){return aggregateId;} String getPayload(){return payload;} String getStatus(){return status;} void setStatus(String v){status=v;}
}

