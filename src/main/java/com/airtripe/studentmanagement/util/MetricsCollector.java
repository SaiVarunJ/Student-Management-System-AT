package com.airtripe.studentmanagement.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class MetricsCollector {
    private final Map<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> totalNanos = new ConcurrentHashMap<>();

    public void record(String name, long nanos) {
        counts.computeIfAbsent(name, k -> new LongAdder()).increment();
        totalNanos.computeIfAbsent(name, k -> new LongAdder()).add(nanos);
    }

    public long getCount(String name) {
        LongAdder a = counts.get(name);
        return a == null ? 0 : a.sum();
    }

    public long getTotalNanos(String name) {
        LongAdder a = totalNanos.get(name);
        return a == null ? 0 : a.sum();
    }

    public Map<String, Long> snapshotCounts() {
        Map<String, Long> out = new ConcurrentHashMap<>();
        counts.forEach((k, v) -> out.put(k, v.sum()));
        return out;
    }

    public Map<String, Long> snapshotAvgMillis() {
        Map<String, Long> out = new ConcurrentHashMap<>();
        counts.forEach((k, v) -> {
            long cnt = v.sum();
            long total = totalNanos.getOrDefault(k, new LongAdder()).sum();
            long avgMs = cnt == 0 ? 0 : (total / cnt) / 1_000_000;
            out.put(k, avgMs);
        });
        return out;
    }
}

