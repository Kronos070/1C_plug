package com.example.edtai.ui.marks;

import com.example.edtai.bootstrap.FeedbackAssembler;
import com.example.edtai.infrastructure.marks.RewriteWatcher;
import com.example.edtai.infrastructure.settings.PreferenceKeys;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;

public final class MarksStartup implements IStartup {

    @Override
    public void earlyStartup() {
        // Запускает отслеживание и видимость меток ИИ
        new RewriteWatcher(FeedbackAssembler.service()).install();
        InstanceScope.INSTANCE.getNode(PreferenceKeys.NODE).addPreferenceChangeListener(event -> {
            if (PreferenceKeys.MARKS_VISIBLE.equals(event.getKey())) {
                PlatformUI.getWorkbench().getDisplay().asyncExec(MarksStartup::sync);
            }
        });
        PlatformUI.getWorkbench().getDisplay().asyncExec(MarksStartup::sync);
        PlatformUI.getWorkbench().getDisplay().asyncExec(
                () -> MarksColor.install(PlatformUI.getWorkbench().getDisplay()));
    }

    private static void sync() {
        // Согласует настройки, редакторы и меню
        boolean visible = MarksVisibility.isVisible();
        MarksVisibility.apply(visible);
        ICommandService commands = PlatformUI.getWorkbench().getService(ICommandService.class);
        Command command = commands == null ? null : commands.getCommand(ToggleMarksHandler.COMMAND);
        State state = command == null ? null : command.getState(RegistryToggleState.STATE_ID);
        if (state != null && !Boolean.valueOf(visible).equals(state.getValue())) {
            state.setValue(visible);
        }
    }
}
