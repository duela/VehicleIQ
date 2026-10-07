package uk.vehicleiq.core;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/v1/support/tickets")
class SupportController {
    private final SupportTicketRepository tickets;private final AuditEventRepository audit;
    SupportController(SupportTicketRepository tickets,AuditEventRepository audit){this.tickets=tickets;this.audit=audit;}
    @GetMapping List<TicketView> list(@AuthenticationPrincipal Jwt jwt){
        List<SupportTicketEntity> rows=VehicleController.isAdmin(jwt)?tickets.findAllByOrderByCreatedAtDesc():tickets.findByTenantIdOrderByCreatedAtDesc(VehicleController.tenant(jwt));
        return rows.stream().map(this::view).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) TicketView create(@Valid @RequestBody TicketRequest request,@AuthenticationPrincipal Jwt jwt){
        String tenant=VehicleController.tenant(jwt);SupportTicketEntity saved=tickets.save(new SupportTicketEntity(tenant,VehicleController.subject(jwt),request.subject(),request.message()));
        audit.save(new AuditEventEntity(tenant,VehicleController.subject(jwt),"support.ticket.created",saved.getId()));return view(saved);
    }
    @PutMapping("/{id}") TicketView update(@PathVariable String id,@RequestBody TicketStatusRequest request,@AuthenticationPrincipal Jwt jwt){
        if(!VehicleController.isAdmin(jwt))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin role required");
        SupportTicketEntity ticket=tickets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found"));
        if(!List.of("OPEN","IN_PROGRESS","RESOLVED").contains(request.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported status");
        ticket.setStatus(request.status());tickets.save(ticket);audit.save(new AuditEventEntity("platform",VehicleController.subject(jwt),"support.ticket.updated",id));return view(ticket);
    }
    private TicketView view(SupportTicketEntity t){return new TicketView(t.getId(),t.getRequester(),t.getSubject(),t.getMessage(),t.getStatus(),t.getPriority(),t.getCreatedAt());}
}
record TicketStatusRequest(String status){}

