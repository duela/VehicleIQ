package uk.vehicleiq.core;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity,String> {
    List<SupportTicketEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<SupportTicketEntity> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}

