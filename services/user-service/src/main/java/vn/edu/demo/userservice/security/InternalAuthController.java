package vn.edu.demo.userservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {
    private final JwtService jwtService;

    public InternalAuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verify(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (!jwtService.isValidBearer(authorization)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.noContent().build();
    }
}
