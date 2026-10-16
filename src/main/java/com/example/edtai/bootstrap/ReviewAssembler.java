package com.example.edtai.bootstrap;

import com.example.edtai.application.ReviewService;
import com.example.edtai.infrastructure.marks.ReviewMarkers;
import com.example.edtai.infrastructure.review.StateReviewStore;
import com.example.edtai.infrastructure.review.WorkspaceReviewFiles;
import com.example.edtai.ui.marks.AnnotationSync;
import java.nio.file.Path;
import org.eclipse.core.runtime.Platform;

public final class ReviewAssembler {

    private static final ReviewService SERVICE = new ReviewService(new StateReviewStore(ReviewAssembler::directory),
            new WorkspaceReviewFiles(), new ReviewMarkers(AnnotationSync::refresh));

    private ReviewAssembler() {
    }

    public static ReviewService service() {
        // Возвращает сервис проверки правок
        return SERVICE;
    }

    private static Path directory() {
        // Возвращает каталог хранения исходников
        return Platform.getStateLocation(Platform.getBundle("com.example.edtai")).toFile().toPath().resolve("review");
    }
}
