package dev.agiro.demo.config;

import dev.agiro.demo.entity.Brand;
import dev.agiro.demo.entity.Customer;
import dev.agiro.demo.entity.Order;
import dev.agiro.demo.entity.Product;
import dev.agiro.demo.repository.BrandJpaRepository;
import dev.agiro.demo.repository.CustomerJpaRepository;
import dev.agiro.demo.repository.OrderJpaRepository;
import dev.agiro.demo.repository.ProductJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Initializes sample data for testing the criteria-filter library.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BrandJpaRepository brandRepository;
    private final ProductJpaRepository productRepository;
    private final OrderJpaRepository orderRepository;
    private final CustomerJpaRepository customerRepository;

    public DataInitializer(BrandJpaRepository brandRepository,
                           ProductJpaRepository productRepository,
                           OrderJpaRepository orderRepository,
                           CustomerJpaRepository customerRepository) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) {
        if (brandRepository.count() > 0) {
            log.info("Data already exists, skipping initialization");
            return;
        }

        log.info("Initializing sample data...");

        // Create brands
        Brand apple = brandRepository.save(new Brand("Apple", "Technology company", "USA", 1976));
        Brand samsung = brandRepository.save(new Brand("Samsung", "Electronics conglomerate", "South Korea", 1938));
        Brand sony = brandRepository.save(new Brand("Sony", "Entertainment and electronics", "Japan", 1946));
        Brand nike = brandRepository.save(new Brand("Nike", "Athletic footwear and apparel", "USA", 1964));
        Brand adidas = brandRepository.save(new Brand("Adidas", "Sportswear manufacturer", "Germany", 1949));
        Brand inactiveBrand = new Brand("OldBrand", "Discontinued brand", "UK", 1900);
        inactiveBrand.setActive(false);
        brandRepository.save(inactiveBrand);

        // Create products
        List<Product> products = List.of(
                new Product("iPhone 15 Pro", "Latest Apple smartphone", new BigDecimal("1199.99"), "Electronics", true, 50, apple),
                new Product("iPhone 14", "Previous gen Apple smartphone", new BigDecimal("799.99"), "Electronics", true, 100, apple),
                new Product("MacBook Pro 16", "Professional laptop", new BigDecimal("2499.99"), "Electronics", true, 25, apple),
                new Product("AirPods Pro", "Wireless earbuds", new BigDecimal("249.99"), "Electronics", true, 200, apple),
                
                new Product("Galaxy S24 Ultra", "Samsung flagship phone", new BigDecimal("1299.99"), "Electronics", true, 75, samsung),
                new Product("Galaxy Tab S9", "Premium Android tablet", new BigDecimal("849.99"), "Electronics", true, 40, samsung),
                new Product("Samsung TV 65\"", "4K OLED Television", new BigDecimal("1599.99"), "Electronics", true, 15, samsung),
                
                new Product("PlayStation 5", "Gaming console", new BigDecimal("499.99"), "Gaming", true, 30, sony),
                new Product("Sony WH-1000XM5", "Noise cancelling headphones", new BigDecimal("399.99"), "Electronics", true, 60, sony),
                new Product("Sony Camera A7IV", "Mirrorless camera", new BigDecimal("2499.99"), "Electronics", true, 10, sony),
                
                new Product("Air Jordan 1", "Classic basketball shoes", new BigDecimal("180.00"), "Footwear", true, 150, nike),
                new Product("Nike Air Max", "Running shoes", new BigDecimal("150.00"), "Footwear", true, 200, nike),
                new Product("Nike Dri-FIT Shirt", "Athletic t-shirt", new BigDecimal("35.00"), "Apparel", true, 500, nike),
                
                new Product("Adidas Ultraboost", "Premium running shoes", new BigDecimal("190.00"), "Footwear", true, 120, adidas),
                new Product("Adidas Originals Hoodie", "Classic hoodie", new BigDecimal("80.00"), "Apparel", true, 300, adidas),
                
                // Inactive products
                new Product("Old iPhone", "Discontinued model", new BigDecimal("299.99"), "Electronics", false, 0, apple),
                new Product("Old Galaxy", "Discontinued model", new BigDecimal("199.99"), "Electronics", false, 0, samsung)
        );
        productRepository.saveAll(products);

        // Create orders
        List<Order> orders = List.of(
                createOrder("ORD-001", "John Doe", "john@example.com", new BigDecimal("1449.98"), 2, Order.Status.DELIVERED),
                createOrder("ORD-002", "Jane Smith", "jane@example.com", new BigDecimal("2499.99"), 1, Order.Status.SHIPPED),
                createOrder("ORD-003", "Bob Wilson", "bob@example.com", new BigDecimal("499.99"), 1, Order.Status.CONFIRMED),
                createOrder("ORD-004", "Alice Brown", "alice@example.com", new BigDecimal("180.00"), 1, Order.Status.PENDING),
                createOrder("ORD-005", "Charlie Davis", "charlie@example.com", new BigDecimal("3699.98"), 3, Order.Status.DELIVERED),
                createOrder("ORD-006", "Diana Miller", "diana@example.com", new BigDecimal("849.99"), 1, Order.Status.CANCELLED),
                createOrder("ORD-007", "Eve Johnson", "eve@example.com", new BigDecimal("1599.99"), 1, Order.Status.SHIPPED),
                createOrder("ORD-008", "Frank Lee", "frank@example.com", new BigDecimal("429.99"), 2, Order.Status.PENDING),
                createOrder("ORD-009", "Grace Kim", "grace@example.com", new BigDecimal("2749.99"), 2, Order.Status.CONFIRMED),
                createOrder("ORD-010", "Henry Chen", "henry@example.com", new BigDecimal("115.00"), 3, Order.Status.DELIVERED)
        );
        orderRepository.saveAll(orders);

        // Create customers with JSONB data
        createCustomers();

        log.info("Sample data initialized: {} brands, {} products, {} orders, {} customers",
                brandRepository.count(), productRepository.count(), orderRepository.count(), customerRepository.count());
    }

    private void createCustomers() {
        // Customer 1: VIP with dark theme
        Customer c1 = new Customer("john.doe@example.com", "John", "Doe", "USA");
        c1.setLoyaltyPoints(5000);
        c1.setPreferences(Map.of(
                "theme", "dark",
                "language", "en",
                "notifications", Map.of("email", true, "sms", false)
        ));
        c1.setMetadata(Map.of(
                "source", "web",
                "campaign", "summer2024",
                "tags", List.of("vip", "early-adopter")
        ));
        c1.setAddress(Map.of(
                "street", "123 Main St",
                "city", "New York",
                "zip", "10001",
                "country", "USA"
        ));
        customerRepository.save(c1);

        // Customer 2: Regular with light theme
        Customer c2 = new Customer("jane.smith@example.com", "Jane", "Smith", "UK");
        c2.setLoyaltyPoints(1500);
        c2.setPreferences(Map.of(
                "theme", "light",
                "language", "en",
                "notifications", Map.of("email", true, "sms", true)
        ));
        c2.setMetadata(Map.of(
                "source", "mobile",
                "campaign", "winter2023",
                "tags", List.of("regular")
        ));
        c2.setAddress(Map.of(
                "street", "456 High St",
                "city", "London",
                "zip", "SW1A 1AA",
                "country", "UK"
        ));
        customerRepository.save(c2);

        // Customer 3: Premium with system theme
        Customer c3 = new Customer("carlos.garcia@example.com", "Carlos", "Garcia", "Spain");
        c3.setLoyaltyPoints(8500);
        c3.setPreferences(Map.of(
                "theme", "system",
                "language", "es",
                "notifications", Map.of("email", false, "sms", true)
        ));
        c3.setMetadata(Map.of(
                "source", "referral",
                "campaign", "spring2024",
                "tags", List.of("vip", "premium", "influencer"),
                "referredBy", "john.doe@example.com"
        ));
        c3.setAddress(Map.of(
                "street", "Calle Mayor 789",
                "city", "Madrid",
                "zip", "28001",
                "country", "Spain"
        ));
        customerRepository.save(c3);

        // Customer 4: New customer with minimal data
        Customer c4 = new Customer("marie.dupont@example.com", "Marie", "Dupont", "France");
        c4.setLoyaltyPoints(100);
        c4.setPreferences(Map.of(
                "theme", "dark",
                "language", "fr"
        ));
        c4.setMetadata(Map.of(
                "source", "web",
                "tags", List.of("new")
        ));
        customerRepository.save(c4);

        // Customer 5: Inactive customer
        Customer c5 = new Customer("old.user@example.com", "Old", "User", "Germany");
        c5.setActive(false);
        c5.setLoyaltyPoints(0);
        c5.setPreferences(Map.of(
                "theme", "light",
                "language", "de"
        ));
        c5.setMetadata(Map.of(
                "source", "web",
                "tags", List.of("churned"),
                "deactivatedReason", "inactive"
        ));
        customerRepository.save(c5);

        // Customer 6: Japanese customer with different preferences
        Customer c6 = new Customer("yuki.tanaka@example.com", "Yuki", "Tanaka", "Japan");
        c6.setLoyaltyPoints(3200);
        c6.setPreferences(Map.of(
                "theme", "dark",
                "language", "ja",
                "notifications", Map.of("email", true, "sms", true, "push", true)
        ));
        c6.setMetadata(Map.of(
                "source", "mobile",
                "campaign", "asia2024",
                "tags", List.of("vip", "mobile-first")
        ));
        c6.setAddress(Map.of(
                "street", "1-2-3 Shibuya",
                "city", "Tokyo",
                "zip", "150-0002",
                "country", "Japan"
        ));
        customerRepository.save(c6);
    }

    private Order createOrder(String orderNumber, String customerName, String customerEmail,
                              BigDecimal totalAmount, Integer itemCount, Order.Status status) {
        Order order = new Order(orderNumber, customerName, customerEmail, totalAmount, itemCount);
        order.setStatus(status);
        return order;
    }
}
