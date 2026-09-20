package com.smartreply.beta;

import java.util.ArrayList;
import java.util.List;

/** Suppress duplicate broadcasts, not a customer's next menu selection. */
public final class ChatbotReplyGate {
    private static final long WINDOW_MS = 60_000L;
    private static final long FALLBACK_INTERVAL_MS = 30_000L;
    private static final int MAX_REPLIES_PER_WINDOW = 20;
    private long windowStartedAt;
    private int replyCount;
    private long lastFallbackAt;
    private final List<String> receipts = new ArrayList<>();

    public ChatbotReplyGate(String saved) {
        if (saved == null || saved.isEmpty()) return;
        String[] values = saved.split("\n");
        try {
            windowStartedAt = Long.parseLong(values[0]);
            replyCount = Integer.parseInt(values[1]);
            lastFallbackAt = Long.parseLong(values[2]);
            for (int i = 3; i < values.length && receipts.size() < 64; i++) {
                receipts.add(values[i]);
            }
        } catch (RuntimeException invalidState) {
            windowStartedAt = 0L;
            replyCount = 0;
            lastFallbackAt = 0L;
            receipts.clear();
        }
    }

    public String blockedReason(String receipt, boolean matched, long now) {
        if (receipts.contains(receipt)) return "Duplicate SMS broadcast ignored";
        if (now < windowStartedAt || now - windowStartedAt >= WINDOW_MS) {
            windowStartedAt = now;
            replyCount = 0;
        }
        if (replyCount >= MAX_REPLIES_PER_WINDOW) return "Reply limit reached; try again in one minute";
        if (!matched && lastFallbackAt > 0L && now >= lastFallbackAt
                && now - lastFallbackAt < FALLBACK_INTERVAL_MS) {
            return "Welcome menu was already sent recently";
        }
        return null;
    }

    public void recordAccepted(String receipt, boolean matched, long now) {
        replyCount++;
        if (!matched) lastFallbackAt = now;
        receipts.add(receipt);
        while (receipts.size() > 64) receipts.remove(0);
    }

    public String save() {
        StringBuilder saved = new StringBuilder().append(windowStartedAt).append('\n')
                .append(replyCount).append('\n').append(lastFallbackAt);
        for (String receipt : receipts) saved.append('\n').append(receipt);
        return saved.toString();
    }
}
