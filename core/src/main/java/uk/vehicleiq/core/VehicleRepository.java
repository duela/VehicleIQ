package uk.vehicleiq.core;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleRepository extends JpaRepository<VehicleEntity,String> {
    List<VehicleEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<VehicleEntity> findAllByOrderByCreatedAtDesc();
    long countByTenantId(String tenantId);
}

