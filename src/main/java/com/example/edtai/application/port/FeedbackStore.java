package com.example.edtai.application.port;

import com.example.edtai.domain.Feedback;
import java.io.IOException;

@FunctionalInterface
public interface FeedbackStore {

    void save(Feedback feedback) throws IOException;
}
