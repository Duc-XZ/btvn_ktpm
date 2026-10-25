package vn.edu.demo.userservice.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.demo.userservice.client.OrderServiceClient;
import vn.edu.demo.userservice.domain.UserDashboard;
import vn.edu.demo.userservice.domain.UserStore;

@RestController
@RequestMapping("/dashboard/users")
public class UserDashboardController {
    private final UserStore users;
    private final OrderServiceClient orders;

    public UserDashboardController(UserStore users, OrderServiceClient orders) {
        this.users = users;
        this.orders = orders;
    }

    @GetMapping("/{userId}")
    public UserDashboard get(@PathVariable String userId) {
        var user = users.findById(userId);
        return new UserDashboard(user, orders.findForUser(userId));
    }
}
