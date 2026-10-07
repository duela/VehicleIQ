package uk.vehicleiq.core;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="evidence_assets", indexes=@Index(name="idx_evidence_tenant", columnList="tenantId"))
class EvidenceAssetEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false) private String tenantId;
    @Column(nullable=false) private String vehicleId;
    @Column(nullable=false) private String filename;
    @Column(nullable=false) private String contentType;
    private String objectKey;
    @Column(nullable=false) private Instant uploadedAt=Instant.now();
    protected EvidenceAssetEntity() {}
    EvidenceAssetEntity(String tenant,String vehicle,String name,String type,String key){tenantId=tenant;vehicleId=vehicle;filename=name;contentType=type;objectKey=key;}
    String getId(){return id;} String getTenantId(){return tenantId;} String getVehicleId(){return vehicleId;} String getFilename(){return filename;} String getContentType(){return contentType;} String getObjectKey(){return objectKey;} Instant getUploadedAt(){return uploadedAt;}
}

