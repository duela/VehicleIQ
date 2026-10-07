package uk.vehicleiq.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1")
class VehicleController {
    private final VehicleRepository vehicles; private final AnalysisRepository analyses; private final AuditEventRepository audit;
    private final EvidenceAssetRepository evidence; private final AnalysisService analysisService; private final ObjectMapper mapper; private final FileStorageService storage;
    VehicleController(VehicleRepository vehicles,AnalysisRepository analyses,AuditEventRepository audit,EvidenceAssetRepository evidence,AnalysisService analysisService,ObjectMapper mapper,FileStorageService storage){
        this.vehicles=vehicles;this.analyses=analyses;this.audit=audit;this.evidence=evidence;this.analysisService=analysisService;this.mapper=mapper;this.storage=storage;
    }
    @GetMapping("/vehicles") List<VehicleView> list(@AuthenticationPrincipal Jwt jwt){
        List<VehicleEntity> rows=isAdmin(jwt)?vehicles.findAllByOrderByCreatedAtDesc():vehicles.findByTenantIdOrderByCreatedAtDesc(tenant(jwt));
        return rows.stream().map(this::view).toList();
    }
    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED)
    VehicleView create(@Valid @RequestBody VehicleRequest request,@AuthenticationPrincipal Jwt jwt){return view(save(request,tenant(jwt),subject(jwt)));}
    @PostMapping("/vehicles/import")
    Map<String,Object> importVehicles(@RequestBody List<@Valid VehicleRequest> rows,@AuthenticationPrincipal Jwt jwt){
        if(rows.size()>250) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Import limit is 250 rows");
        List<VehicleView> imported=rows.stream().map(row->view(save(row,tenant(jwt),subject(jwt)))).toList();
        return Map.of("imported",imported.size(),"vehicles",imported);
    }
    @PostMapping(value="/vehicles/{id}/evidence",consumes="multipart/form-data")
    Map<String,Object> upload(@PathVariable String id,@RequestPart("file") MultipartFile file,@AuthenticationPrincipal Jwt jwt){
        VehicleEntity vehicle=owned(id,jwt);
        try {
            String type=file.getContentType()==null?"application/octet-stream":file.getContentType();
            if(!Set.of("image/jpeg","image/png","image/webp","application/pdf").contains(type)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Upload a JPG, PNG, WebP or PDF file.");
            if(file.getSize()>12L*1024*1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Maximum file size is 12 MB.");
            String key=storage.store(file); EvidenceAssetEntity asset=evidence.save(new EvidenceAssetEntity(vehicle.getTenantId(),id,file.getOriginalFilename()==null?"evidence":file.getOriginalFilename(),type,key));
            audit.save(new AuditEventEntity(vehicle.getTenantId(),subject(jwt),"evidence.uploaded",asset.getId()));
            return Map.of("id",asset.getId(),"filename",asset.getFilename(),"contentType",asset.getContentType(),"uploadedAt",asset.getUploadedAt());
        } catch(ResponseStatusException e){throw e;} catch(Exception e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not store evidence.");}
    }
    @GetMapping("/vehicles/{id}/evidence")
    List<Map<String,Object>> evidenceList(@PathVariable String id,@AuthenticationPrincipal Jwt jwt){
        owned(id,jwt); return evidence.findByVehicleIdOrderByUploadedAtDesc(id).stream().map(e->Map.<String,Object>of("id",e.getId(),"filename",e.getFilename(),"contentType",e.getContentType(),"uploadedAt",e.getUploadedAt())).toList();
    }
    @PostMapping("/vehicles/{id}/analyses") @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String,Object> requestAnalysis(@PathVariable String id,@AuthenticationPrincipal Jwt jwt){
        AnalysisEntity result=analysisService.request(id,tenant(jwt),subject(jwt));
        return Map.of("id",result.getId(),"vehicleId",id,"status",result.getStatus(),"requestedAt",result.getRequestedAt(),"statusUrl","/v1/analyses/"+result.getId());
    }
    private VehicleEntity save(VehicleRequest r,String tenant,String actor){
        VehicleEntity v=new VehicleEntity();v.setTenantId(tenant);v.setRegistration(r.registration().trim().toUpperCase(Locale.UK));v.setMake(r.make().trim());v.setModel(r.model().trim());
        v.setYear(r.year());v.setMileage(r.mileage());v.setFuelType(r.fuelType());v.setTransmission(r.transmission());v.setColour(r.colour());v.setPurchasePrice(r.purchasePrice());
        v.setBuyerFees(r.buyerFees());v.setTransport(r.transport());v.setRepairEstimate(r.repairEstimate());v.setConditionNotes(r.conditionNotes()==null?"":r.conditionNotes());
        try{v.setComparablePricesJson(mapper.writeValueAsString(r.comparablePrices()==null?List.of():r.comparablePrices()));}catch(Exception ex){throw new IllegalArgumentException("Invalid comparable prices");}
        VehicleEntity saved=vehicles.save(v);audit.save(new AuditEventEntity(tenant,actor,"vehicle.created",saved.getId()));return saved;
    }
    private VehicleEntity owned(String id,Jwt jwt){return vehicles.findById(id).filter(v->isAdmin(jwt)||v.getTenantId().equals(tenant(jwt))).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Vehicle not found"));}
    private VehicleView view(VehicleEntity v){
        AnalysisEntity latest=analyses.findByVehicleIdOrderByRequestedAtDesc(v.getId()).stream().findFirst().orElse(null);
        try{return new VehicleView(v.getId(),v.getRegistration(),v.getMake(),v.getModel(),v.getYear(),v.getMileage(),v.getFuelType(),v.getTransmission(),v.getColour(),v.getPurchasePrice(),v.getBuyerFees(),v.getTransport(),v.getRepairEstimate(),mapper.readValue(v.getComparablePricesJson(),new TypeReference<>(){}),v.getConditionNotes(),v.getCreatedAt(),latest==null?null:latest.getStatus(),latest==null?null:latest.getId());}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    static String tenant(Jwt jwt){return Optional.ofNullable(jwt.getClaimAsString("tenant_id")).orElse("tenant-demo");}
    static String subject(Jwt jwt){return Optional.ofNullable(jwt.getSubject()).orElse("vehicleiq-user");}
    static boolean isAdmin(Jwt jwt){return jwt.getClaimAsStringList("roles")!=null&&jwt.getClaimAsStringList("roles").contains("ADMIN");}
}

