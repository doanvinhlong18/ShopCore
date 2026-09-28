package com.shopcore.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "debug=false")
class ApiGatewayApplicationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void shouldLoadExpectedRoutes() {
        var routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes).isNotNull().hasSize(6);

        Map<String, URI> destinations = routes.stream()
                .collect(Collectors.toMap(route -> route.getId(), route -> route.getUri()));

        assertThat(destinations)
                .containsEntry("user-auth", URI.create("http://localhost:8081"))
                .containsEntry("user-profile", URI.create("http://localhost:8081"))
                .containsEntry("product-products", URI.create("http://localhost:8082"))
                .containsEntry("product-categories", URI.create("http://localhost:8082"))
                .containsEntry("product-inventory", URI.create("http://localhost:8082"))
                .containsEntry("order", URI.create("http://localhost:8083"));

        Map<String, String> expectedPaths = Map.of(
                "user-auth", "/api/auth/**",
                "user-profile", "/api/users/**",
                "product-products", "/api/products/**",
                "product-categories", "/api/categories/**",
                "product-inventory", "/api/inventory/**",
                "order", "/api/orders/**"
        );

        routes.forEach(route -> assertThat(route.getPredicates())
                .anySatisfy(predicate -> assertThat(predicate.getArgs())
                        .containsValue(expectedPaths.get(route.getId()))));
    }
}
