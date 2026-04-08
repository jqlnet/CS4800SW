package edu.cpp.cs4800.receipttracker;

import edu.cpp.cs4800.receipttracker.model.UserSettings;
import edu.cpp.cs4800.receipttracker.model.UserSettingsRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SettingsController {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @GetMapping("/settings")
    public String showSettings(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        // Load or create settings for this user
        UserSettings settings = userSettingsRepository.findById(uid)
                .orElse(new UserSettings(uid, (String) session.getAttribute("userName")));

        model.addAttribute("title", "Settings");
        model.addAttribute("settings", settings);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("displayName", settings.getDisplayName() != null
                ? settings.getDisplayName()
                : session.getAttribute("userName"));
        return "settings";
    }

    @PostMapping("/settings")
    public String saveSettings(HttpSession session,
                               @RequestParam String displayName,
                               @RequestParam(required = false) String emailNotifications) {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        UserSettings settings = userSettingsRepository.findById(uid)
                .orElse(new UserSettings(uid, displayName));

        settings.setDisplayName(displayName.trim());
        settings.setEmailNotifications("on".equals(emailNotifications));
        userSettingsRepository.save(settings);

        // Update session display name
        session.setAttribute("displayName", displayName.trim());

        return "redirect:/settings";
    }
}