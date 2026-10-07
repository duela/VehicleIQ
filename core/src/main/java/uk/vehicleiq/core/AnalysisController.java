package uk.vehicleiq.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1")
class AnalysisController {
    private final AnalysisRepository analyses;private final VehicleRepository vehicles;private final AnalysisService service;private final ObjectMapper mapper;
    @Value("${vehicleiq.worker-token}") private String workerToken;
    AnalysisController(AnalysisRepository analyses,VehicleRepository vehicles,AnalysisService service,ObjectMapper mapper){this.analyses=analyses;this.vehicles=vehicles;this.service=service;this.mapper=mapper;}
    @GetMapping("/analyses") List<AnalysisView> list(@AuthenticationPrincipal Jwt jwt){
        List<AnalysisEntity> all=VehicleController.isAdmin(jwt)?analyses.findAllByOrderByRequestedAtDesc():analyses.findByTenantIdOrderByRequestedAtDesc(VehicleController.tenant(jwt));
        return all.stream().map(this::view).toList();
    }
    @GetMapping("/analyses/{id}") AnalysisView get(@PathVariable String id,@AuthenticationPrincipal Jwt jwt){return view(owned(id,jwt));}
    @PostMapping("/internal/analysis/{id}/complete")
    AnalysisView completeFromWorker(@PathVariable String id,@RequestHeader(value="X-Worker-Token",required=false) String supplied){
        if(!Objects.equals(workerToken,supplied))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Worker authentication failed");
        return view(service.complete(id));
    }
    @PostMapping("/partner/vehicle-assessments") @PreAuthorize("hasRole('PARTNER')") @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String,Object> partnerAssessment(@RequestBody @jakarta.validation.Valid VehicleRequest request,@AuthenticationPrincipal Jwt jwt){
        VehicleEntity vehicle=new VehicleEntity();vehicle.setTenantId(VehicleController.tenant(jwt));vehicle.setRegistration(request.registration().trim().toUpperCase(Locale.UK));vehicle.setMake(request.make().trim());vehicle.setModel(request.model().trim());vehicle.setYear(request.year());vehicle.setMileage(request.mileage());vehicle.setFuelType(request.fuelType());vehicle.setTransmission(request.transmission());vehicle.setColour(request.colour());vehicle.setPurchasePrice(request.purchasePrice());vehicle.setBuyerFees(request.buyerFees());vehicle.setTransport(request.transport());vehicle.setRepairEstimate(request.repairEstimate());vehicle.setConditionNotes(request.conditionNotes()==null?"":request.conditionNotes());
        try{vehicle.setComparablePricesJson(mapper.writeValueAsString(request.comparablePrices()==null?List.of():request.comparablePrices()));}catch(Exception e){throw new IllegalArgumentException(e);}
        vehicle=vehicles.save(vehicle);AnalysisEntity analysis=service.request(vehicle.getId(),VehicleController.tenant(jwt),VehicleController.subject(jwt));
        return Map.of("id",analysis.getId(),"vehicleId",vehicle.getId(),"status","QUEUED","statusUrl","/partner/v1/vehicle-assessments/"+analysis.getId());
    }
    @GetMapping("/partner/vehicle-assessments/{id}") @PreAuthorize("hasRole('PARTNER')")
    AnalysisView partnerGet(@PathVariable String id,@AuthenticationPrincipal Jwt jwt){return view(analyses.findById(id).filter(a->a.getTenantId().equals(VehicleController.tenant(jwt))).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Assessment not found")));}
    private AnalysisEntity owned(String id,Jwt jwt){return analyses.findById(id).filter(a->VehicleController.isAdmin(jwt)||a.getTenantId().equals(VehicleController.tenant(jwt))).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Analysis not found"));}
    private AnalysisView view(AnalysisEntity a){try{return new AnalysisView(a.getId(),a.getVehicleId(),a.getStatus(),a.getEngineVersion(),mapper.readValue(a.getResultJson(),new TypeReference<>(){}),a.getRequestedAt(),a.getCompletedAt());}catch(Exception e){throw new IllegalStateException(e);}}
}

