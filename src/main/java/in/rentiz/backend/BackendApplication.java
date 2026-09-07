package in.rentiz.backend;

import in.rentiz.backend.Entity.Products;

import in.rentiz.backend.Repository.ProductRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
    }
	@Bean
	CommandLineRunner newrunner( ProductRepository productRepository){

		return nus ->{
			Products product1 = new Products();
			product1.Setobject("Shampoo", 62.5F);

			productRepository.save(product1);
			System.out.println("Product saved successfully to Supabase!");
		};
	};


}
