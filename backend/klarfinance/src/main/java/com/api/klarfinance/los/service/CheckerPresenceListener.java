package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.api.klarfinance.auth.repository.UserRepository;

import java.security.Principal;

/** Checker online begitu konek WebSocket (buka halaman Approval), offline begitu disconnect. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckerPresenceListener {
    private final CheckerAssignmentService checkerAssignmentService;
    private final UserRepository userRepository;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Principal principal = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        withCheckerIdentity(principal, checkerAssignmentService::markOnline);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        withCheckerIdentity(principal, checkerAssignmentService::markOffline);
    }

    private void withCheckerIdentity(Principal principal, java.util.function.Consumer<String> action) {
        if (principal == null) return;
        userRepository.findByIdentity(principal.getName()).ifPresent(user -> {
            if (user.getRole() != null && "CHECKER".equalsIgnoreCase(user.getRole().getName())) {
                action.accept(principal.getName());
            }
        });
    }
}
