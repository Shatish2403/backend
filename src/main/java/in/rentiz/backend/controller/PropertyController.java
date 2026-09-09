package in.rentiz.backend.controller;


import in.rentiz.backend.entity.Property;
import in.rentiz.backend.service.PropertyService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController()
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService){
        this.propertyService = propertyService;
    };

    @PostMapping
    public ResponseEntity<Property> createProperty(@RequestBody Property property){
        Property created = propertyService.createProperty(property);
        return ResponseEntity.ok(created);
    }

    @GetMapping()
    public ResponseEntity<List<Property>> getPropertyByCity(@RequestParam String cityId){
        List<Property> properties = propertyService.getPropertiesForCity(cityId);
        return ResponseEntity.ok(properties);
    }

    @GetMapping("/public/{publicid}")
    public ResponseEntity<Property> getPropertyByPublicId(@PathVariable String publicId){
        return propertyService.getPropertyByPublicId(publicId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}

