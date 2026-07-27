package dev.agiro.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Criteria Filter Demo API",
                version = "1.0",
                description = """
                        Demo application showcasing all features of the criteria-filter library.
                        
                        ## Features Demonstrated
                        
                        ### 1. AbstractFilterController (Products)
                        Classic inheritance-based approach for exposing filter endpoints.
                        
                        ### 2. @EnableFilterEndpoint (Brands)
                        Annotation-based automatic endpoint registration at class level.
                        
                        ### 3. @FilterSearch/@FilterSchema (Orders)
                        Method-level annotations for maximum flexibility.
                        
                        ### 4. Filter Interceptors
                        - **AuditLogInterceptor**: Logs all queries (applies to all entities)
                        - **ActiveOnlyInterceptor**: Auto-adds 'active=true' filter (Products only)
                        
                        ## Filter Operators
                        
                        | Operator | Description | Example |
                        |----------|-------------|---------|
                        | EQ | Equals | `{"field": "status", "operator": "EQ", "value": "ACTIVE"}` |
                        | NE | Not equals | `{"field": "status", "operator": "NE", "value": "DELETED"}` |
                        | GT | Greater than | `{"field": "price", "operator": "GT", "value": 100}` |
                        | GTE | Greater or equal | `{"field": "price", "operator": "GTE", "value": 100}` |
                        | LT | Less than | `{"field": "price", "operator": "LT", "value": 50}` |
                        | LTE | Less or equal | `{"field": "price", "operator": "LTE", "value": 50}` |
                        | LIKE | Contains | `{"field": "name", "operator": "LIKE", "value": "phone"}` |
                        | IN | In list | `{"field": "category", "operator": "IN", "values": ["A", "B"]}` |
                        | BETWEEN | Range | `{"field": "price", "operator": "BETWEEN", "values": [10, 100]}` |
                        | IS_NULL | Is null | `{"field": "deletedAt", "operator": "IS_NULL"}` |
                        | IS_NOT_NULL | Not null | `{"field": "email", "operator": "IS_NOT_NULL"}` |
                        """
        )
)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
