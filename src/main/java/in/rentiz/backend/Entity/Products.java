package in.rentiz.backend.Entity;
import jakarta.persistence.*;


@Entity
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private Float price;

    public String GetId(){
        return id;
    }
    public void SetId(String id){
        this.id = id;
    }
    public String GetName(){
        return name;
    }
    public void SetName(String name){
        this.name = name;
    }
    public Float GetPrice(){
        return price;
    }
    public void SetPrice(Float x){
        this.price =x;
    }
    public void Setobj(String name, Float x){
        this.name = name;
        this.price = x;
    }


}
