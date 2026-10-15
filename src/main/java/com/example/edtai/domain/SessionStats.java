package com.example.edtai.domain;

public record SessionStats(int userMessages, int assistantMessages, int toolCalls, long tokens, double cost,
        int contextPercent) {
}
