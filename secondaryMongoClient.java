spring:
  data:
    mongodb:
      primary:
        uri: mongodb://localhost:27017/primarydb
      secondary:
        uri: mongodb://localhost:27017/secondarydb

@Configuration
@EnableMongoRepositories(
    basePackages = "com.example.repository.primary",
    mongoTemplateRef = "primaryMongoTemplate"
)
public class PrimaryMongoConfig {

    @Primary
    @Bean(name = "primaryMongoClient")
    public MongoClient primaryMongoClient() {
        return MongoClients.create("mongodb://localhost:27017/primarydb");
    }

    @Primary
    @Bean(name = "primaryMongoTemplate")
    public MongoTemplate primaryMongoTemplate() {
        return new MongoTemplate(primaryMongoClient(), "primarydb");
    }
}





@Configuration
@EnableMongoRepositories(
    basePackages = "com.example.repository.secondary",
    mongoTemplateRef = "secondaryMongoTemplate"
)
public class SecondaryMongoConfig {

    @Bean(name = "secondaryMongoClient")
    public MongoClient secondaryMongoClient() {
        return MongoClients.create("mongodb://localhost:27017/secondarydb");
    }

    @Bean(name = "secondaryMongoTemplate")
    public MongoTemplate secondaryMongoTemplate() {
        return new MongoTemplate(secondaryMongoClient(), "secondarydb");
    }
}




@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    private String email;
    
    // constructors, getters, setters
}


@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private Double price;
    
    // constructors, getters, setters
}

package com.example.repository.primary;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByName(String name);
}

package com.example.repository.secondary;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByPriceGreaterThan(Double price);
}

@Service
public class DataService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    @Qualifier("primaryMongoTemplate")
    private MongoTemplate primaryMongoTemplate;
    
    @Autowired
    @Qualifier("secondaryMongoTemplate")
    private MongoTemplate secondaryMongoTemplate;

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    // Using MongoTemplate directly
    public List<User> findUsersByCustomQuery() {
        Query query = new Query(Criteria.where("email").regex("@gmail.com"));
        return primaryMongoTemplate.find(query, User.class);
    }

    public List<Product> findExpensiveProducts() {
        Query query = new Query(Criteria.where("price").gt(100));
        return secondaryMongoTemplate.find(query, Product.class);
    }
}



@RestController
@RequestMapping("/api")
public class DataController {

    @Autowired
    private DataService dataService;

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = dataService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = dataService.saveProduct(product);
        return ResponseEntity.ok(savedProduct);
    }

    @GetMapping("/users/gmail")
    public ResponseEntity<List<User>> getGmailUsers() {
        List<User> users = dataService.findUsersByCustomQuery();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/products/expensive")
    public ResponseEntity<List<Product>> getExpensiveProducts() {
        List<Product> products = dataService.findExpensiveProducts();
        return ResponseEntity.ok(products);
    }
}




