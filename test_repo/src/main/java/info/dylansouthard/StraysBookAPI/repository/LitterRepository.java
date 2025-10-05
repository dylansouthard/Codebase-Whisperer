package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LitterRepository extends JpaRepository<Litter, Long> {
    @Query(value = """
SELECT * FROM litters
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radius)
""", nativeQuery = true)
    public List<Litter> findByLocation(@Param("latitude") double latitude, @Param("longitude") double longitude, @Param("radius") double radius);
}
