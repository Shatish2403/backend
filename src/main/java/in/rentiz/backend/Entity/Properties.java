package in.rentiz.backend.Entity;
import in.rentiz.backend.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name="properties")
public class Properties {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;


    private PropType proptype;

    private Availability availability;
    private Facing facing;
    private FurnishingType furnishingType;
    private PrefferedTenants prefferedTenants;
    private PropAge propAge;
    private PropType propType;
    private SpaceType spaceType;
    private Cities city;


    private String title;
    private String description;

    private Double longitude;
    private Double latitude;

    private Integer rent;
    private Integer deposit;
    private Integer buildup_area;
    private Integer floor;
    private Integer total_floor;
    private Integer flags;


    private Boolean islease;
    private Boolean iscouple;
    private Boolean isnonveg_allowed;




    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDate available_from;







    @ManyToMany
    @JoinTable(
            name = "property_amenties",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name="amenty_id")
    )
    private Set<Amenties> amentiesList;



}
