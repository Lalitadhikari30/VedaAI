package com.vedaai.assessment.store;

import com.vedaai.assessment.model.AssessmentSession;
import com.vedaai.assessment.model.AssessmentStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for assessment sessions.
 * Sessions are evicted after TTL (30 min by default).
 */
@Component
public class InMemoryAssessmentStore {

    private final ConcurrentHashMap<String, AssessmentSession> sessions = new ConcurrentHashMap<>();
    private static final Duration TTL = Duration.ofMinutes(30);

    public void save(AssessmentSession session) {
        sessions.put(session.getAssessmentId(), session);
    }

    public Optional<AssessmentSession> findById(String assessmentId) {
        return Optional.ofNullable(sessions.get(assessmentId));
    }

    public void updateStatus(String assessmentId, AssessmentStatus status, String progress) {
        sessions.computeIfPresent(assessmentId, (id, session) -> {
            session.setStatus(status);
            session.setProgress(progress);
            return session;
        });
    }

    public void updateStatusWithError(String assessmentId, AssessmentStatus status, String errorMessage) {
        sessions.computeIfPresent(assessmentId, (id, session) -> {
            session.setStatus(status);
            session.setErrorMessage(errorMessage);
            return session;
        });
    }

    /**
     * Evict expired sessions every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    public void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        sessions.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().isBefore(cutoff));
    }
}
