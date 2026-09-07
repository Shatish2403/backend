package in.rentiz.backend.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


@Entity
@Table(name = "Users")
public class Users {

    @Id
    @GeneratedValue(GenerationType.UUID)
    private String user_id;

    private String name;

    @NotBlank("email field is mandatory")
    @Email("invalid email id")
    private String email;




}
