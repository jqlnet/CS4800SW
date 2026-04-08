package edu.cpp.cs4800.receipttracker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import edu.cpp.cs4800.receipttracker.model.User;
import edu.cpp.cs4800.receipttracker.model.UserRepository;
import edu.cpp.cs4800.receipttracker.model.UserSettings;
import edu.cpp.cs4800.receipttracker.model.UserSettingsRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/auth/firebase")
    public ResponseEntity<String> authenticateFirebase(
            @RequestParam String idToken,
            HttpSession session) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid   = decoded.getUid();
            String email = decoded.getEmail();
            String name  = decoded.getName();

            // Save or update user
            User user = userRepository.findById(uid).orElse(new User(uid, email, name));
            user.setEmail(email);
            user.setName(name);
            userRepository.save(user);

            // Load display name from settings if set
            UserSettings settings = userSettingsRepository.findById(uid).orElse(null);
            String displayName = (settings != null && settings.getDisplayName() != null)
                    ? settings.getDisplayName()
                    : name;

            // Store in session
            session.setAttribute("uid", uid);
            session.setAttribute("userName", name);
            session.setAttribute("userEmail", email);
            session.setAttribute("displayName", displayName);

            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token: " + e.getMessage());
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}