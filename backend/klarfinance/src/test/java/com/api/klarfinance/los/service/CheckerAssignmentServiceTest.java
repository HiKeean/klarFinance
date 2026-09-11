package com.api.klarfinance.los.service;

import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.LimitApplication;
import com.api.klarfinance.los.repository.LimitApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckerAssignmentServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private LimitApplicationRepository limitApplicationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SimpUserRegistry simpUserRegistry;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    private CheckerAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new CheckerAssignmentService(redis, limitApplicationRepository, messagingTemplate, simpUserRegistry);
    }

    @Test
    void isAvailable_trueWhenOnlineAndNotBusy() {
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("los:checker:online", "checker1")).thenReturn(true);
        when(redis.hasKey("los:checker:current:checker1")).thenReturn(false);

        assertThat(service.isAvailable("checker1")).isTrue();
    }

    @Test
    void isAvailable_falseWhenOffline() {
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("los:checker:online", "checker1")).thenReturn(false);

        assertThat(service.isAvailable("checker1")).isFalse();
    }

    @Test
    void isAvailable_falseWhenOnlineButBusy() {
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("los:checker:online", "checker1")).thenReturn(true);
        when(redis.hasKey("los:checker:current:checker1")).thenReturn(true);

        assertThat(service.isAvailable("checker1")).isFalse();
    }

    @Test
    void currentOwner_returnsOwnerFromRedis() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:owner:5")).thenReturn("checker1");

        Optional<String> owner = service.currentOwner(5);

        assertThat(owner).contains("checker1");
    }

    @Test
    void currentOwner_emptyWhenNoOwner() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:owner:5")).thenReturn(null);

        assertThat(service.currentOwner(5)).isEmpty();
    }

    @Test
    void currentAssignment_parsesApplicationIdFromRedis() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:current:checker1")).thenReturn("42");

        assertThat(service.currentAssignment("checker1")).contains(42);
    }

    @Test
    void handleExpiry_noReassignWhenApplicationAlreadyDecided() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:owner:5")).thenReturn("checker1");
        LimitApplication decided = LimitApplication.builder().id(5).status(EngineStatus.APPROVED).build();
        when(limitApplicationRepository.findById(5)).thenReturn(Optional.of(decided));

        service.handleExpiry(5);

        verify(redis).delete("los:checker:current:checker1");
        verify(redis).delete("los:checker:owner:5");
        // Should NOT try to notify/reassign since application is no longer PENDING_CHECKER
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void handleExpiry_reassignsWhenApplicationStillPendingChecker() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:owner:5")).thenReturn("checker1");
        LimitApplication pending = LimitApplication.builder().id(5).status(EngineStatus.PENDING_CHECKER).build();
        when(limitApplicationRepository.findById(5)).thenReturn(Optional.of(pending));
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("los:checker:online")).thenReturn(Set.of("checker2"));
        when(setOperations.isMember("los:checker:online", "checker2")).thenReturn(true);
        when(redis.hasKey("los:checker:current:checker2")).thenReturn(false);
        when(simpUserRegistry.getUser(anyString())).thenReturn(mock(SimpUser.class));

        service.handleExpiry(5);

        verify(valueOperations).set(eq("los:checker:current:checker2"), eq("5"));
        verify(valueOperations).set(eq("los:checker:owner:5"), eq("checker2"));
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/assignment"), any());
    }

    @Test
    void handleExpiry_noAvailableCheckerLeavesApplicationUnassigned() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:owner:5")).thenReturn("checker1");
        LimitApplication pending = LimitApplication.builder().id(5).status(EngineStatus.PENDING_CHECKER).build();
        when(limitApplicationRepository.findById(5)).thenReturn(Optional.of(pending));
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("los:checker:online")).thenReturn(Set.of());
        when(simpUserRegistry.getUser(anyString())).thenReturn(null);

        service.handleExpiry(5);

        verify(valueOperations, never()).set(eq("los:checker:owner:5"), anyString());
    }

    @Test
    void startReviewIfNeeded_setsLockOnlyOnce() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("los:checker:lock:5", "checker1", CheckerAssignmentService.LOCK_TTL))
                .thenReturn(true);

        service.startReviewIfNeeded(5, "checker1");

        verify(redis).delete("los:checker:offer:5");
        verify(redis).expire("los:checker:current:checker1", CheckerAssignmentService.LOCK_TTL);
        verify(redis).expire("los:checker:owner:5", CheckerAssignmentService.LOCK_TTL);
    }

    @Test
    void startReviewIfNeeded_idempotentWhenAlreadyStarted() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("los:checker:lock:5", "checker1", CheckerAssignmentService.LOCK_TTL))
                .thenReturn(false);

        service.startReviewIfNeeded(5, "checker1");

        verify(redis, never()).delete("los:checker:offer:5");
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void markOnline_addsToOnlineSetAndTriesAssignment() {
        when(redis.opsForSet()).thenReturn(setOperations);
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
        lenient().when(setOperations.isMember("los:checker:online", "checker1")).thenReturn(true);
        lenient().when(redis.hasKey("los:checker:current:checker1")).thenReturn(false);
        lenient().when(limitApplicationRepository.findByStatusOrderByCreatedAtAsc(EngineStatus.PENDING_CHECKER)).thenReturn(java.util.List.of());

        service.markOnline("checker1");

        verify(setOperations).add("los:checker:online", "checker1");
    }

    @Test
    void release_deletesLockKeysAndReassignsToOwner() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("los:checker:owner:5")).thenReturn("checker1");
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("los:checker:online", "checker1")).thenReturn(true);
        when(redis.hasKey("los:checker:current:checker1")).thenReturn(false);
        when(limitApplicationRepository.findByStatusOrderByCreatedAtAsc(EngineStatus.PENDING_CHECKER)).thenReturn(java.util.List.of());

        service.release(5);

        verify(redis).delete("los:checker:current:checker1");
        verify(redis).delete("los:checker:lock:5");
        verify(redis).delete("los:checker:owner:5");
        verify(redis).delete("los:checker:offer:5");
    }
}
