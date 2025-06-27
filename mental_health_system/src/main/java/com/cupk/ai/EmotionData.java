package com.cupk.ai;

import java.util.List;

public record EmotionData(
        String emotion,
        int intensity,
        List<String> triggers,
        String risk_level
) {}
