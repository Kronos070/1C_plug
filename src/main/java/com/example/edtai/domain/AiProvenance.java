package com.example.edtai.domain;

public record AiProvenance(String model, String prompt, String source) {

    public AiProvenance {
        model = model == null ? "" : model;
        prompt = prompt == null ? "" : prompt;
        source = source == null ? "" : source;
    }
}
