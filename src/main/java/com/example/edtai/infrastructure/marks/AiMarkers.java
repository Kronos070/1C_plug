package com.example.edtai.infrastructure.marks;

import com.example.edtai.domain.AiProvenance;
import java.util.List;
import java.util.UUID;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public final class AiMarkers {

    public static final String TYPE = "com.example.edtai.aiFragment";
    public static final String FRAGMENT = "fragmentId";
    public static final String SNAPSHOT = "snapshot";
    public static final String MODEL = "model";
    public static final String PROMPT = "prompt";
    public static final String SOURCE = "source";
    public static final String RATING = "rating";
    public static final String REWRITTEN = "rewritten";

    private static final int SNAPSHOT_LIMIT = 20_000;
    private static final int PROMPT_LIMIT = 2_000;

    private AiMarkers() {
    }

    public static IMarker create(IFile file, int start, int end, String code, AiProvenance provenance)
            throws CoreException {
        // Создает метку кода ИИ на диапазоне
        IMarker marker = file.createMarker(TYPE);
        marker.setAttributes(new String[] {IMarker.CHAR_START, IMarker.CHAR_END, IMarker.MESSAGE, FRAGMENT,
            SNAPSHOT, MODEL, PROMPT, SOURCE}, new Object[] {start, end, message(provenance),
            UUID.randomUUID().toString(), limit(code, SNAPSHOT_LIMIT), provenance.model(),
            limit(provenance.prompt(), PROMPT_LIMIT), provenance.source()});
        return marker;
    }

    public static List<IMarker> find(IFile file) {
        // Возвращает метки кода ИИ в файле
        try {
            return file.exists() ? List.of(file.findMarkers(TYPE, false, IResource.DEPTH_ZERO)) : List.of();
        } catch (CoreException e) {
            return List.of();
        }
    }

    public static AiProvenance provenance(IMarker marker) {
        // Читает происхождение кода из метки
        return new AiProvenance(marker.getAttribute(MODEL, ""), marker.getAttribute(PROMPT, ""),
                marker.getAttribute(SOURCE, ""));
    }

    public static String message(AiProvenance provenance) {
        // Формирует подсказку для поля редактора
        String model = provenance.model().isEmpty() ? "" : " · " + provenance.model();
        return "Код Перуна" + model + " · ПКМ на поле — оценить";
    }

    private static String limit(String text, int max) {
        // Обрезает длинный текст атрибута
        String value = text == null ? "" : text;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
