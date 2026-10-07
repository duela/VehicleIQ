package uk.vehicleiq.core;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

record VehicleRequest(@NotBlank @Size(max=16) String registration, @NotBlank String make,
                      @NotBlank String model, @NotNull @Min(1950) @Max(2035) Integer year,
                      @Min(0) Integer mileage, String fuelType, String transmission, String colour,
                      @PositiveOrZero double purchasePrice, @PositiveOrZero double buyerFees,
                      @PositiveOrZero double transport, @PositiveOrZero double repairEstimate,
                      List<@Positive Double> comparablePrices, String conditionNotes) {}
record VehicleView(String id,String registration,String make,String model,Integer year,Integer mileage,
                   String fuelType,String transmission,String colour,double purchasePrice,double buyerFees,
                   double transport,double repairEstimate,List<Double> comparablePrices,String conditionNotes,
                   Instant createdAt,String latestAnalysisStatus,String latestAnalysisId) {}
record AnalysisView(String id,String vehicleId,String status,String engineVersion,Map<String,Object> result,
                    Instant requestedAt,Instant completedAt) {}
record TicketRequest(@NotBlank @Size(max=140) String subject,@NotBlank @Size(max=5000) String message) {}
record TicketView(String id,String requester,String subject,String message,String status,String priority,Instant createdAt) {}
record AuditView(String id,String actor,String action,String resourceId,Instant occurredAt) {}
record DashboardView(long vehiclesTotal,long analysesCompleted,long openTickets,double portfolioValue,List<VehicleView> latestVehicles) {}

