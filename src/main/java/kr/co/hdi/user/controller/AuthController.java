package kr.co.hdi.user.controller;

import jakarta.servlet.http.HttpSession;
import kr.co.hdi.user.domain.Role;
import kr.co.hdi.user.dto.request.AuthRequest;
import kr.co.hdi.user.dto.response.AuthResponse;
import kr.co.hdi.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody AuthRequest request,
            HttpSession session
    ) {
        log.debug("Login request received for email: {}", request.email());
        AuthResponse response = authService.loginOrRegister(request.email(), request.password());

        session.setAttribute("userId", response.id());
        session.setAttribute("email", response.email());
        session.setAttribute("role", response.role());

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

        return ResponseEntity.ok(new AuthResponse(userId, email, role));
    }

}
