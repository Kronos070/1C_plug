package com.example.edtai.ui.layout;

import java.util.List;
import java.util.Optional;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.IWorkbenchPartSite;

public final class ChatSlot {

    public static final String VIEW_ID = "com.example.edtai.view.chat";

    private static final String STACK_ID = "com.example.edtai.stack";
    private static final String PLACED_KEY = "com.example.edtai.placed";
    private static final String EDITOR_AREA = "org.eclipse.ui.editorss";
    private static final String MINIMIZED = "Minimized";
    private static final float WIDTH_RATIO = 0.34f;

    private final EModelService models;
    private final MPerspective perspective;

    public ChatSlot(EModelService models, MPerspective perspective) {
        this.models = models;
        this.perspective = perspective;
    }

    public static Optional<Runnable> pendingMove(IWorkbenchPartSite site) {
        // Возвращает перенос открытого вида
        EModelService models = site.getService(EModelService.class);
        MPart part = site.getService(MPart.class);
        MPlaceholder placeholder = part == null ? null : part.getCurSharedRef();
        MPerspective perspective = models == null || placeholder == null ? null : models.getPerspectiveFor(placeholder);
        if (perspective == null) {
            return Optional.empty();
        }
        ChatSlot slot = new ChatSlot(models, perspective);
        return slot.needsMove(placeholder) ? Optional.of(() -> slot.moveToNewStack(placeholder)) : Optional.empty();
    }

    public void prepare(EPartService parts, boolean again) {
        // Готовит место для чата на всю высоту
        if (again) {
            perspective.getPersistedState().remove(PLACED_KEY);
        }
        if (isPlaced()) {
            return;
        }
        Optional<MPlaceholder> existing = placeholder();
        if (existing.isEmpty()) {
            MPlaceholder created = parts.createSharedPart(VIEW_ID);
            created.setToBeRendered(false);
            moveToNewStack(created);
        } else if (needsMove(existing.get())) {
            moveToNewStack(existing.get());
        }
        markPlaced();
    }

    public boolean needsMove(MPlaceholder placeholder) {
        // Проверяет, что чат стоит не на всю высоту
        Object parent = placeholder.getParent();
        if (isPlaced() || !(parent instanceof MPartStack stack)) {
            return false;
        }
        return stack.getTags().contains(MINIMIZED) || !isFullHeight(stack);
    }

    public void moveToNewStack(MPlaceholder placeholder) {
        // Переносит чат в отдельный стек справа
        MPartSashContainerElement anchor = anchor();
        if (anchor == null) {
            return;
        }
        MPartStack stack = models.createModelElement(MPartStack.class);
        stack.setElementId(STACK_ID);
        stack.setToBeRendered(placeholder.isToBeRendered());
        models.insert(stack, anchor, EModelService.RIGHT_OF, WIDTH_RATIO);
        if (placeholder.getParent() == null) {
            stack.getChildren().add(placeholder);
        } else {
            models.move(placeholder, stack);
        }
        if (placeholder.isToBeRendered()) {
            stack.setSelectedElement(placeholder);
        }
        markPlaced();
    }

    public Optional<MPlaceholder> placeholder() {
        // Находит место чата в перспективе
        List<MPlaceholder> found = models.findElements(perspective, VIEW_ID, MPlaceholder.class, null);
        return found.stream().findFirst();
    }

    private boolean isPlaced() {
        // Проверяет отметку о размещении
        return perspective.getPersistedState().containsKey(PLACED_KEY);
    }

    private void markPlaced() {
        // Запоминает размещение в перспективе
        perspective.getPersistedState().put(PLACED_KEY, Boolean.TRUE.toString());
    }

    private boolean isFullHeight(MUIElement start) {
        // Проверяет отсутствие соседей сверху и снизу
        Object parent = start.getParent();
        while (parent instanceof MUIElement container && parent != perspective) {
            if (parent instanceof MPartSashContainer sash && !sash.isHorizontal() && renderedChildren(sash) > 1) {
                return false;
            }
            parent = container.getParent();
        }
        return true;
    }

    private MPartSashContainerElement anchor() {
        // Находит колонку редактора во всю высоту
        if (perspective.getChildren().isEmpty()) {
            return null;
        }
        MPartSashContainerElement root = perspective.getChildren().get(0);
        MUIElement editor = models.find(EDITOR_AREA, perspective);
        if (!(root instanceof MPartSashContainer sash) || !sash.isHorizontal() || editor == null) {
            return root;
        }
        MUIElement current = editor;
        Object parent = current.getParent();
        while (parent instanceof MUIElement container && parent != sash) {
            current = container;
            parent = container.getParent();
        }
        return parent == sash && current instanceof MPartSashContainerElement column ? column : root;
    }

    private static long renderedChildren(MPartSashContainer sash) {
        // Считает отображаемых детей контейнера
        return sash.getChildren().stream().filter(MUIElement::isToBeRendered).count();
    }
}
