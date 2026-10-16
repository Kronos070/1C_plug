package com.example.edtai.ui.docs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public final class DocsEditorInput implements IEditorInput {

    static final DocsEditorInput INSTANCE = new DocsEditorInput();

    private DocsEditorInput() {
    }

    @Override
    public boolean exists() {
        // Не восстанавливается после перезапуска
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        // Возвращает иконку вкладки
        return ImageDescriptor.createFromURL(DocsEditorInput.class.getResource("/icons/perun.png"));
    }

    @Override
    public String getName() {
        // Возвращает заголовок вкладки
        return "Документация";
    }

    @Override
    public IPersistableElement getPersistable() {
        // Состояние вкладки не сохраняется
        return null;
    }

    @Override
    public String getToolTipText() {
        // Возвращает подсказку вкладки
        return "Документация конфигурации ПерУН";
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        // Адаптеры не поддерживаются
        return null;
    }
}
