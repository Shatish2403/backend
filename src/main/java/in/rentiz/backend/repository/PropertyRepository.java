package in.rentiz.backend.repository;

import in.rentiz.backend.entity.Property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByCityIdAndArchivedFalseAndSuspendedFalseOrCityIdAndArchivedFalseAndSuspendedUntilBefore(
            String cityId1, String cityId2, OffsetDateTime now
    );

    List<Property> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<Property> findByPublicId(String publicId);
}