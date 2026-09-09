package in.rentiz.backend.service;


import in.rentiz.backend.entity.Property;
import in.rentiz.backend.repository.PropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.annotation.JsonAppend;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PropertyService{

    private PropertyRepository propertyRepository;

    public PropertyService (PropertyRepository propertyRepository){
      this.propertyRepository = propertyRepository;
    };

    @Transactional
    public Property createProperty(Property property){

        return propertyRepository.save(property);
    };

    public Optional<Property> getPropertyByPublicId(String publicId){
        return propertyRepository.findByPublicId(publicId);
    };
    public List<Property> getPropertiesForCity(String cityId){
        OffsetDateTime now = OffsetDateTime.now();
      return propertyRepository.findByCityIdAndArchivedFalseAndSuspendedFalseOrCityIdAndArchivedFalseAndSuspendedUntilBefore(cityId,cityId,now);
    };
    public List<Property> getOwnerListings(UUID ownerId){
        return propertyRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    };
};
