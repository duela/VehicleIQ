package uk.vehicleiq.core;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1")
class DashboardController {
    private final VehicleRepository vehicles;private final AnalysisRepository analyses;private final SupportTicketRepository tickets;private final AuditEventRepository audit;
    DashboardController(VehicleRepository vehicles,AnalysisRepository analyses,SupportTicketRepository tickets,AuditEventRepository audit){this.vehicles=vehicles;this.analyses=analyses;this.tickets=tickets;this.audit=audit;}
    @GetMapping("/dashboard") DashboardView dashboard(@AuthenticationPrincipal Jwt jwt){
        String tenant=VehicleController.tenant(jwt);List<VehicleEntity> all=VehicleController.isAdmin(jwt)?vehicles.findAllByOrderByCreatedAtDesc():vehicles.findByTenantIdOrderByCreatedAtDesc(tenant);
        List<AnalysisEntity> work=VehicleController.isAdmin(jwt)?analyses.findAllByOrderByRequestedAtDesc():analyses.findByTenantIdOrderByRequestedAtDesc(tenant);
        List<VehicleView> latest=all.stream().limit(8).map(v->{AnalysisEntity a=analyses.findByVehicleIdOrderByRequestedAtDesc(v.getId()).stream().findFirst().orElse(null);return new VehicleView(v.getId(),v.getRegistration(),v.getMake(),v.getModel(),v.getYear(),v.getMileage(),v.getFuelType(),v.getTransmission(),v.getColour(),v.getPurchasePrice(),v.getBuyerFees(),v.getTransport(),v.getRepairEstimate(),List.of(),v.getConditionNotes(),v.getCreatedAt(),a==null?null:a.getStatus(),a==null?null:a.getId());}).toList();
        long completed=work.stream().filter(a->"COMPLETED".equals(a.getStatus())).count();double portfolio=all.stream().mapToDouble(VehicleEntity::getPurchasePrice).sum();
        return new DashboardView(all.size(),completed,tickets.countByStatus("OPEN"),portfolio,latest);
    }
    @GetMapping("/admin/dashboard") @PreAuthorize("hasRole('ADMIN')") Map<String,Object> adminDashboard(){
        List<AnalysisEntity> all=analyses.findAllByOrderByRequestedAtDesc();return Map.of("tenants",2,"vehicles",vehicles.count(),"analyses",all.size(),"queued",all.stream().filter(a->"QUEUED".equals(a.getStatus())).count(),"openTickets",tickets.countByStatus("OPEN"),"connectors",List.of(Map.of("id","manual","name","Manual and CSV data","status","ACTIVE","mode","User-provided data"),Map.of("id","partner-mock","name","Partner adapter template","status","NOT_CONNECTED","mode","Mock only; no live partner access")));
    }
    @GetMapping("/admin/audit") @PreAuthorize("hasRole('ADMIN')") List<AuditView> auditEvents(){return audit.findTop50ByOrderByOccurredAtDesc().stream().map(a->new AuditView(a.getId(),a.getActor(),a.getAction(),a.getResourceId(),a.getOccurredAt())).toList();}
}

