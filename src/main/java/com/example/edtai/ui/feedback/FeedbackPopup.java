package com.example.edtai.ui.feedback;

import com.example.edtai.domain.FeedbackKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

final class FeedbackPopup {

    private static final List<String> CATEGORIES = List.of("Ошибка", "Запрос в цикле", "Не по стандарту", "Лишнее",
            "Выдуман метод", "Другое");

    private FeedbackPopup() {
    }

    static void open(CodeTarget target, FeedbackKind initial, Consumer<FeedbackInput> onSubmit) {
        // Показывает окно отзыва под кодом
        StyledText text = textWidget(target);
        Shell owner = text != null ? text.getShell() : target.editor().getSite().getShell();
        Shell shell = new Shell(owner, SWT.ON_TOP | SWT.TOOL | SWT.BORDER);
        shell.setText("Отзыв о коде Перуна");
        shell.setLayout(new GridLayout(1, false));
        Label title = new Label(shell, SWT.NONE);
        title.setText("Отзыв о коде Перуна");
        Composite ratings = row(shell);
        Button like = toggle(ratings, "👍 Хорошо", initial == FeedbackKind.LIKE);
        Button dislike = toggle(ratings, "👎 Плохо", initial != FeedbackKind.LIKE);
        like.addListener(SWT.Selection, event -> dislike.setSelection(!like.getSelection()));
        dislike.addListener(SWT.Selection, event -> like.setSelection(!dislike.getSelection()));
        Composite chips = row(shell);
        List<Button> categories = new ArrayList<>();
        CATEGORIES.forEach(name -> categories.add(toggle(chips, name, false)));
        new Label(shell, SWT.NONE).setText("Комментарий (необязательно):");
        Text comment = new Text(shell, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        comment.setMessage("Комментарий (необязательно)");
        GridData commentData = new GridData(SWT.FILL, SWT.FILL, true, true);
        commentData.heightHint = 64;
        comment.setLayoutData(commentData);
        Composite actions = row(shell);
        Button send = new Button(actions, SWT.PUSH);
        send.setText("Отправить (Ctrl+Enter)");
        Button cancel = new Button(actions, SWT.PUSH);
        cancel.setText("Отмена");
        Runnable submit = () -> {
            List<String> chosen = categories.stream().filter(Button::getSelection).map(Button::getText).toList();
            onSubmit.accept(new FeedbackInput(like.getSelection() ? FeedbackKind.LIKE : FeedbackKind.DISLIKE,
                    chosen, comment.getText().strip()));
            shell.dispose();
        };
        send.addListener(SWT.Selection, event -> submit.run());
        cancel.addListener(SWT.Selection, event -> shell.dispose());
        Listener keys = event -> {
            if (shell.isDisposed() || event.display.getActiveShell() != shell) {
                return;
            }
            boolean enter = event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR;
            if (event.keyCode == SWT.ESC) {
                shell.dispose();
            } else if (enter && (event.stateMask & SWT.MOD1) != 0) {
                event.doit = false;
                submit.run();
            }
        };
        shell.getDisplay().addFilter(SWT.KeyDown, keys);
        shell.addListener(SWT.Dispose, event -> event.display.removeFilter(SWT.KeyDown, keys));
        place(shell, text);
        shell.open();
        comment.setFocus();
    }

    private static Composite row(Composite parent) {
        // Создает строку элементов с переносом
        Composite row = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = true;
        layout.marginLeft = 0;
        row.setLayout(layout);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return row;
    }

    private static Button toggle(Composite parent, String label, boolean selected) {
        // Создает кнопку-переключатель
        Button button = new Button(parent, SWT.TOGGLE);
        button.setText(label);
        button.setSelection(selected);
        return button;
    }

    private static StyledText textWidget(CodeTarget target) {
        // Находит текстовое поле редактора
        Control control = target.editor().getAdapter(Control.class);
        if (control instanceof StyledText text) {
            return text;
        }
        Object operations = target.editor().getAdapter(ITextOperationTarget.class);
        return operations instanceof ITextViewer viewer ? viewer.getTextWidget() : null;
    }

    private static void place(Shell shell, StyledText text) {
        // Размещает окно у курсора в пределах экрана
        Point anchor = shell.getDisplay().getCursorLocation();
        if (text != null && !text.isDisposed()) {
            Point caret = text.toDisplay(text.getLocationAtOffset(text.getCaretOffset()));
            anchor = new Point(caret.x, caret.y + text.getLineHeight() + 4);
        }
        Point size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Rectangle screen = shell.getDisplay().getPrimaryMonitor().getClientArea();
        for (Monitor monitor : shell.getDisplay().getMonitors()) {
            if (monitor.getClientArea().contains(anchor)) {
                screen = monitor.getClientArea();
            }
        }
        int width = Math.min(size.x, screen.width - 16);
        int height = Math.min(shell.computeSize(width, SWT.DEFAULT).y, screen.height - 16);
        int x = Math.max(screen.x + 8, Math.min(anchor.x, screen.x + screen.width - width - 8));
        boolean below = anchor.y + height <= screen.y + screen.height;
        int y = below ? anchor.y : Math.max(screen.y + 8, anchor.y - height - 40);
        shell.setBounds(x, y, width, height);
    }
}
