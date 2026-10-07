package uk.vehicleiq.core;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="vehicles", indexes={@Index(name="idx_vehicle_tenant", columnList="tenantId"), @Index(name="idx_vehicle_registration", columnList="registration")})
class VehicleEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false) private String tenantId;
    @Column(nullable=false) private String registration;
    @Column(nullable=false) private String make;
    @Column(nullable=false) private String model;
    @Column(name="model_year") private Integer year;
    private Integer mileage;
    private String fuelType;
    private String transmission;
    private String colour;
    @Column(nullable=false) private double purchasePrice;
    private double buyerFees;
    private double transport;
    private double repairEstimate;
    @Lob private String comparablePricesJson="[]";
    @Lob private String conditionNotes="";
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected VehicleEntity() {}
    String getId(){return id;} String getTenantId(){return tenantId;} void setTenantId(String v){tenantId=v;}
    String getRegistration(){return registration;} void setRegistration(String v){registration=v;}
    String getMake(){return make;} void setMake(String v){make=v;} String getModel(){return model;} void setModel(String v){model=v;}
    Integer getYear(){return year;} void setYear(Integer v){year=v;} Integer getMileage(){return mileage;} void setMileage(Integer v){mileage=v;}
    String getFuelType(){return fuelType;} void setFuelType(String v){fuelType=v;} String getTransmission(){return transmission;} void setTransmission(String v){transmission=v;}
    String getColour(){return colour;} void setColour(String v){colour=v;} double getPurchasePrice(){return purchasePrice;} void setPurchasePrice(double v){purchasePrice=v;}
    double getBuyerFees(){return buyerFees;} void setBuyerFees(double v){buyerFees=v;} double getTransport(){return transport;} void setTransport(double v){transport=v;}
    double getRepairEstimate(){return repairEstimate;} void setRepairEstimate(double v){repairEstimate=v;} String getComparablePricesJson(){return comparablePricesJson;} void setComparablePricesJson(String v){comparablePricesJson=v;}
    String getConditionNotes(){return conditionNotes;} void setConditionNotes(String v){conditionNotes=v;} Instant getCreatedAt(){return createdAt;}
}
