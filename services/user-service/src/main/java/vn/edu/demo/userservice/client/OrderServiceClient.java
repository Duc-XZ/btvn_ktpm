package vn.edu.demo.userservice.client;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import vn.edu.demo.userservice.domain.OrderSummary;

@Component
public class OrderServiceClient {
    private final RestClient restClient;

    public OrderServiceClient(
            RestClient.Builder builder,
            @Value("${orders.base-url}") String ordersBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder
                .baseUrl(ordersBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public List<OrderSummary> findForUser(String userId) {
        try {
            OrderSummary[] orders = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/orders").queryParam("userId", userId).build())
                    .retrieve()
                    .body(OrderSummary[].class);
            return orders == null ? List.of() : List.of(orders);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Order Service is unavailable",
                    exception);
        }
    }
}
