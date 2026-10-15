package com.example.edtai.ui.marks;

import com.example.edtai.infrastructure.marks.AiMarkers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public final class AiAnnotationImages implements IAnnotationImageProvider {

    private static final String BUNDLE = "com.example.edtai";
    private static final Map<String, Image> IMAGES = new ConcurrentHashMap<>();

    @Override
    public Image getManagedImage(Annotation annotation) {
        // Выбирает значок метки по оценке
        String name = "ai-mark";
        if (annotation instanceof MarkerAnnotation marked && marked.getMarker().exists()) {
            IMarker marker = marked.getMarker();
            String rating = marker.getAttribute(AiMarkers.RATING, "");
            if (marker.getAttribute(AiMarkers.REWRITTEN, false)) {
                name = "ai-rewrite";
            } else if ("like".equals(rating)) {
                name = "ai-like";
            } else if ("dislike".equals(rating)) {
                name = "ai-dislike";
            }
        }
        return IMAGES.computeIfAbsent(name, AiAnnotationImages::load);
    }

    @Override
    public String getImageDescriptorId(Annotation annotation) {
        // Не использует реестр изображений
        return null;
    }

    @Override
    public ImageDescriptor getImageDescriptor(String imageDescriptorId) {
        // Не использует реестр изображений
        return null;
    }

    private static Image load(String name) {
        // Загружает значок из плагина
        return ResourceLocator.imageDescriptorFromBundle(BUNDLE, "icons/" + name + ".png")
                .map(ImageDescriptor::createImage).orElse(null);
    }
}
