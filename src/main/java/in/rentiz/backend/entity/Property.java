package in.rentiz.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import in.rentiz.backend.enums.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "properties")

public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id",nullable = false, unique = true,length = 8)
    private String publicId;

    @Column(name="owner_id")
    private UUID ownerId;

    @Column(name="city_id", nullable = false, length = 40)
    private String cityId;

    @Column(name="title", nullable = false, length = 120)
    private String title;

    @Column(name="description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name="bhk_type", nullable = false)
    private BhkType bhktype;

    @Column(name = "rent",nullable = false)
    private Integer rent;

    @Column(name = "deposit",nullable = false)
    private Integer deposit;

    @Column(name = "is_lease", nullable = false)
    private Boolean isLease = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private  PropertyType propertyType = PropertyType.APARTMENT;


    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing", nullable = false)
    private FurnishingType furnishing = FurnishingType.SEMI;


    @Column(name = "parking_2w",nullable = false)
    private Boolean parking2w = false;

    @Column(name = "parking_4w",nullable = false)
    private Boolean parking4w = false;

    @Column(name = "built_up_area_sqft")
    private Integer builtUpAreaSqft;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_age")
    private PropertyAge propertyAge;


    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "total_floors")
    private Integer totalFloors;


    @Enumerated(EnumType.STRING)
    @Column(name = "availability",nullable = false)
    private AvailabilityBucket availability = AvailabilityBucket.IMMEDIATE;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_tenants",nullable = false)
    private PreferredTenant preferredTenants;

    @Column(name = " non_veg_allowed", nullable = false)
    private Boolean nonVegAllowed = true;

    @Column(name = "has_gym", nullable = false)
    private Boolean hasGym = false;

    @Column(name = "hosting_visit_next_2_days",nullable = false)
    private Boolean hostingVisitNext2Days = false;

    @Column(name = "image_urls",columnDefinition = "text[]", nullable = false)
    private String[] imageUrls;

    @Column(name = "area",length = 120)
    private String area;

    @Column(name = "lat",nullable = false)
    private Double lat;

    @Column(name = "lng",nullable = false)
    private Double lng;

    @Column(name="owner_name",nullable = false)
    private String ownerName;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "owner_whatsapp", nullable = false, length = 10)
    private String ownerWhatsapp;

    @Column(name = "flag_count",nullable = false)
    private Integer flagCount = 0;

    @Column(name="suspended", nullable = false)
    private Boolean suspended = false;

    @Column(name = "suspended_until")
    private OffsetDateTime suspendedUntil;

    @Column(name = "verified_by_admin", nullable = false)
    private Boolean verifiedByAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "appeal_status",nullable = false)
    private AppealStatus appealStatus = AppealStatus.NONE;

    @Column(name = "appeal_note")
    private String appealNote;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "listed_by",nullable = false)
    private ListedBy listedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at",nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();



}