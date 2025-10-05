package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.SterilizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SterilizationStatusRepository extends JpaRepository<SterilizationStatus, Long> {
}
