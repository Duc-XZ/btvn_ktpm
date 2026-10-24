package vn.edu.demo.userservice.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.edu.demo.userservice.domain.User;
import vn.edu.demo.userservice.domain.UserRequest;
import vn.edu.demo.userservice.domain.UserStore;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserStore users;

    public UserController(UserStore users) {
        this.users = users;
    }

    @GetMapping
    public List<User> list() {
        return users.findAll();
    }

    @GetMapping("/{userId}")
    public User get(@PathVariable String userId) {
        return users.findById(userId);
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody UserRequest request) {
        User created = users.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{userId}")
    public User update(@PathVariable String userId, @Valid @RequestBody UserRequest request) {
        return users.update(userId, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        users.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
