package com.example.edtai.application;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.PermissionStore;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.PermissionScope;
import com.example.edtai.domain.UiAnswer;
import com.example.edtai.domain.UiRequest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PermissionGuard {

    private static final Pattern TOOL = Pattern.compile("'([^']+)'");

    private final AgentGateway gateway;
    private final PermissionStore store;
    private final Consumer<ChatEvent> emit;
    private final Set<String> session = ConcurrentHashMap.newKeySet();
    private final Map<String, String> pending = new ConcurrentHashMap<>();

    PermissionGuard(AgentGateway gateway, PermissionStore store, Consumer<ChatEvent> emit) {
        this.gateway = gateway;
        this.store = store;
        this.emit = emit;
    }

    boolean intercept(ChatEvent event) {
        // Отвечает агенту по сохраненному правилу
        if (!(event instanceof ChatEvent.InputRequested input) || input.request().kind() != UiRequest.Kind.CONFIRM) {
            return false;
        }
        String key = keyOf(input.request());
        boolean allow = session.contains(key) || store.allowed().contains(key);
        boolean deny = store.denied().contains(key);
        if (!allow && !deny) {
            pending.put(input.request().id(), key);
            return false;
        }
        gateway.answer(UiAnswer.confirm(input.request().id(), allow && !deny));
        String verdict = allow && !deny ? "Разрешено" : "Запрещено";
        emit.accept(new ChatEvent.Notice("info", verdict + " по сохраненному правилу: " + key));
        return true;
    }

    void remember(UiAnswer answer, PermissionScope scope) {
        // Запоминает решение пользователя
        String key = pending.remove(answer.id());
        if (key == null || answer.confirmed() == null) {
            return;
        }
        switch (scope) {
            case SESSION -> session.add(key);
            case ALWAYS -> store.remember(key, true);
            case NEVER -> store.remember(key, false);
            default -> {
            }
        }
    }

    void resetSession() {
        // Сбрасывает разрешения текущей сессии
        session.clear();
    }

    private static String keyOf(UiRequest request) {
        // Определяет инструмент из запроса агента
        Matcher matcher = TOOL.matcher(request.title());
        return matcher.find() ? matcher.group(1) : request.title();
    }
}
