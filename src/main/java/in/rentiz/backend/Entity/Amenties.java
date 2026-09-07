package in.rentiz.backend.Entity;


import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "amenties")
public class Amenties {

    @Id
    @GeneratedValue(GenerationType.UUID)
    private String uuid;
    private String name;

    @ManyToMany(mappedBy = "amentiesList")
    private Set<Properties> Property;

}
