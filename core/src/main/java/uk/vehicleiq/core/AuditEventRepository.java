package uk.vehicleiq.core;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity,String> {
    List<AuditEventEntity> findTop50ByOrderByOccurredAtDesc();
    List<AuditEventEntity> findTop50ByTenantIdOrderByOccurredAtDesc(String tenantId);
}

