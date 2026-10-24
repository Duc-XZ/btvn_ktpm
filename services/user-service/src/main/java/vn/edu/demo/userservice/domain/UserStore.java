package vn.edu.demo.userservice.domain;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserStore {
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    public UserStore() {
        users.put("u-123", new User("u-123", "Linh Nguyen", "linh@example.com", null));
        users.put("u-456", new User("u-456", "Minh Tran", "minh@example.com", null));
    }

    public List<User> findAll() {
        return users.values().stream().sorted(Comparator.comparing(User::id)).toList();
    }

    public User findById(String id) {
        User user = users.get(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    public User create(UserRequest request) {
        String id = UUID.randomUUID().toString();
        User user = new User(id, request.name(), request.email(), request.avatarKey());
        users.put(id, user);
        return user;
    }

    public User update(String id, UserRequest request) {
        if (!users.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User updated = new User(id, request.name(), request.email(), request.avatarKey());
        users.put(id, updated);
        return updated;
    }

    public void delete(String id) {
        if (users.remove(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }
}
