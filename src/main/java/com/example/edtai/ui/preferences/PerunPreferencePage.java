package com.example.edtai.ui.preferences;

import com.example.edtai.infrastructure.settings.PreferenceKeys;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public final class PerunPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public PerunPreferencePage() {
        super(GRID);
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, PreferenceKeys.NODE));
        setDescription("Подключение ИИ-помощника ПерУН. Пустая команда — автопоиск perun, затем pi в PATH.\n"
                + "Можно указать путь к perun, pi или perun.bundle.mjs. Изменения применяются сразу.");
    }

    @Override
    public void init(IWorkbench workbench) {
        // Инициализирует страницу настроек
        setTitle("ПерУН");
    }

    @Override
    protected void createFieldEditors() {
        // Создает поля настроек агента
        addField(new StringFieldEditor(PreferenceKeys.COMMAND, "Команда запуска агента:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceKeys.ARGUMENTS, "Дополнительные аргументы:", getFieldEditorParent()));
        addField(new DirectoryFieldEditor(PreferenceKeys.WORK_DIRECTORY,
                "Рабочий каталог (пусто — автоопределение проекта):", getFieldEditorParent()));
        addField(new ComboFieldEditor(PreferenceKeys.THEME, "Тема чата:", new String[][] {
            {"Как в EDT", "auto"}, {"Тёмная", "dark"}, {"Светлая", "light"}}, getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceKeys.ALLOWED_TOOLS, "Всегда разрешать инструменты:",
                getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceKeys.DENIED_TOOLS, "Никогда не разрешать:", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceKeys.DEMO_MODE, "Демо-режим без агента (ответы-заглушки)",
                getFieldEditorParent()));
    }
}
