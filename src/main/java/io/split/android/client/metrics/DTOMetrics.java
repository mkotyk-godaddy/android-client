package io.split.android.client.metrics;

import io.split.android.client.dtos.Counter;
import io.split.android.client.dtos.Latency;

interface DTOMetrics {
    void time(Latency dto);

    void count(Counter dto);
}
