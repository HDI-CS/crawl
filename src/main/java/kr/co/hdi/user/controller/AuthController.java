package kr.co.hdi.user.controller;

import jakarta.servlet.http.HttpSession;
import kr.co.hdi.user.domain.Role;
import kr.co.hdi.user.dto.RegisterRequest;
import kr.co.hdi.user.dto.request.LoginRequest;
import kr.co.hdi.user.dto.response.AuthInfoResponse;
import kr.co.hdi.user.dto.response.AuthResponse;
import kr.co.hdi.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpSession session
    ) {
        log.debug("Login request received for email: {}", request.email());
        AuthResponse response = authService.login(request.email(), request.password());

        session.setAttribute("userId", response.id());
        session.setAttribute("email", response.email());
        session.setAttribute("role", response.role());
        session.setAttribute("name", response.name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String email = (String) session.getAttribute("email");
        Role role = (Role) session.getAttribute("role");
        String name = (String) session.getAttribute("name");

        AuthResponse response = AuthResponse.of(userId, email, name, role);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me/info")
    public ResponseEntity<AuthInfoResponse> getCurrentUserInfo(
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {

        AuthInfoResponse response = authService.getAuthInfo(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.createUser(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(
            @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.createAdmin(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
