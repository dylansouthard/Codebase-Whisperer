package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {


    @Query(value = """
SELECT * FROM animals
WHERE ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radius)
""", nativeQuery = true)
    public List<Animal> findByLocation(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius);



    @Query("SELECT a from Animal a WHERE a.id = :id AND a.shouldAppear = true")
    Optional<Animal> findByActiveId(@Param("id") long id);

    @Query("""
    Select a from User u
    JOIN u.watchedAnimals a
    WHERE u.id = :userId
    ORDER BY a.createdAt DESC, a.id DESC
""")
    Page<Animal> findPaginatedWatchedAnimalsPaginated(
            @Param("userId") Long userId,
            Pageable pageable);


    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM User u
    JOIN u.watchedAnimals a
    WHERE u.id = :userId AND a.id = :animalId
""")
    Boolean isWatchedByUser(@Param("userId") Long userId, @Param("animalId") Long animalId);

    @Query("""
        select a.id
        from User u
        join u.watchedAnimals a
        where u.id = :userId and a.id in :animalIds
    """)
    List<Long> findWatchedIdsIn(@Param("userId") Long userId,
                                @Param("animalIds") List<Long> animalIds);
}
