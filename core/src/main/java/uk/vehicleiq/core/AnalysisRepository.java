package uk.vehicleiq.core;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AnalysisRepository extends JpaRepository<AnalysisEntity,String> {
    List<AnalysisEntity> findByTenantIdOrderByRequestedAtDesc(String tenantId);
    List<AnalysisEntity> findAllByOrderByRequestedAtDesc();
    List<AnalysisEntity> findByVehicleIdOrderByRequestedAtDesc(String vehicleId);
    long countByTenantIdAndStatus(String tenantId,String status);
}

