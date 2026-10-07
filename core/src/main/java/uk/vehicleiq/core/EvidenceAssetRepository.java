package uk.vehicleiq.core;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EvidenceAssetRepository extends JpaRepository<EvidenceAssetEntity,String> {
    List<EvidenceAssetEntity> findByVehicleIdOrderByUploadedAtDesc(String vehicleId);
}
