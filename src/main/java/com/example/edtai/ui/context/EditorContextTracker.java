package com.example.edtai.ui.context;

import com.example.edtai.domain.CodeContext;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

public final class EditorContextTracker {

    private final IWorkbenchPage page;
    private final ISelectionService selections;
    private final Consumer<Optional<CodeContext>> onChange;
    private final ISelectionListener selectionListener = this::onSelection;
    private final IPartListener2 partListener = new IPartListener2() {
        @Override
        public void partActivated(IWorkbenchPartReference reference) {
            // Обновляет контекст при смене редактора
            refresh(null);
        }

        @Override
        public void partClosed(IWorkbenchPartReference reference) {
            // Сбрасывает контекст закрытого редактора
            refresh(reference.getPart(false));
        }

        @Override
        public void partInputChanged(IWorkbenchPartReference reference) {
            // Обновляет контекст при смене файла
            refresh(null);
        }
    };
    private Optional<CodeContext> current = Optional.empty();
    private Optional<CodeContext> pinned = Optional.empty();

    public EditorContextTracker(IWorkbenchPage page, Consumer<Optional<CodeContext>> onChange) {
        this.page = page;
        this.selections = page.getWorkbenchWindow().getSelectionService();
        this.onChange = onChange;
        selections.addPostSelectionListener(selectionListener);
        page.addPartListener(partListener);
        current = Optional.ofNullable(page.getActiveEditor()).flatMap(EditorContextReader::read);
        onChange.accept(current);
    }

    public Optional<CodeContext> resolve(String id) {
        // Находит прикрепленный контекст по идентификатору
        Optional<CodeContext> fresh = Optional.ofNullable(page.getActiveEditor()).flatMap(EditorContextReader::read)
                .filter(context -> context.id().equals(id));
        return fresh.or(() -> Stream.of(current, pinned).flatMap(Optional::stream)
                .filter(context -> context.id().equals(id))
                .findFirst());
    }

    public void pin(CodeContext context) {
        // Запоминает контекст действия из меню
        pinned = Optional.of(context);
    }

    public void dispose() {
        // Отписывается от событий редакторов
        selections.removePostSelectionListener(selectionListener);
        page.removePartListener(partListener);
    }

    private void onSelection(IWorkbenchPart part, ISelection selection) {
        // Обновляет контекст по выделению в редакторе
        if (part instanceof IEditorPart editor) {
            update(EditorContextReader.read(editor, selection));
        }
    }

    private void refresh(IWorkbenchPart closing) {
        // Перечитывает контекст активного редактора
        IEditorPart editor = page.getActiveEditor();
        update(editor == null || editor == closing ? Optional.empty() : EditorContextReader.read(editor));
    }

    private void update(Optional<CodeContext> next) {
        // Сообщает об изменившемся контексте
        if (!next.map(CodeContext::id).equals(current.map(CodeContext::id))) {
            current = next;
            onChange.accept(next);
        }
    }
}
