package com.example.edtai.ui.feedback;

import com.example.edtai.domain.FeedbackKind;
import java.util.List;

record FeedbackInput(FeedbackKind kind, List<String> categories, String comment) {
}
