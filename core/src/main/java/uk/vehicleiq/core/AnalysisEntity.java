package uk.vehicleiq.core;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="analyses", indexes={@Index(name="idx_analysis_tenant", columnList="tenantId"), @Index(name="idx_analysis_vehicle", columnList="vehicleId")})
class AnalysisEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false) private String tenantId;
    @Column(nullable=false) private String vehicleId;
    @Column(nullable=false) private String status="QUEUED";
    @Column(nullable=false) private String engineVersion="rules-1.0.0";
    @Lob private String resultJson="{}";
    @Column(nullable=false) private Instant requestedAt=Instant.now();
    private Instant completedAt;
    protected AnalysisEntity() {}
    AnalysisEntity(String tenant,String vehicle){tenantId=tenant;vehicleId=vehicle;}
    String getId(){return id;} String getTenantId(){return tenantId;} String getVehicleId(){return vehicleId;} String getStatus(){return status;}
    void setStatus(String v){status=v;} String getEngineVersion(){return engineVersion;} String getResultJson(){return resultJson;} void setResultJson(String v){resultJson=v;}
    Instant getRequestedAt(){return requestedAt;} Instant getCompletedAt(){return completedAt;} void setCompletedAt(Instant v){completedAt=v;}
}

