package com.example.edtai.infrastructure.rpc;

import com.example.edtai.infrastructure.process.AgentProcess;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class RpcConnection {

    private static final long TIMEOUT_SECONDS = 60;

    private final AgentProcess process;
    private final Gson gson = new Gson();
    private final AtomicLong ids = new AtomicLong();
    private final Map<String, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();

    public RpcConnection(AgentProcess process) {
        this.process = process;
    }

    public void open(Consumer<JsonObject> events, Runnable onClosed) {
        // Начинает читать вывод агента
        JsonlReader.start(process.stdout(), line -> dispatch(line, events), () -> {
            failPending("Процесс агента завершился");
            onClosed.run();
        });
    }

    public CompletableFuture<JsonObject> request(String type, JsonObject payload, boolean timed) {
        // Отправляет команду и ждет ответ
        String id = "edt-" + ids.incrementAndGet();
        JsonObject command = payload == null ? new JsonObject() : payload.deepCopy();
        command.addProperty("id", id);
        command.addProperty("type", type);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            write(command);
        } catch (IOException e) {
            pending.remove(id);
            future.completeExceptionally(new RpcException("Не удалось отправить команду агенту: " + e.getMessage()));
        }
        CompletableFuture<JsonObject> result = timed ? future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS) : future;
        return result.whenComplete((value, error) -> pending.remove(id));
    }

    public void send(JsonObject record) {
        // Отправляет запись без ожидания ответа
        try {
            write(record);
        } catch (IOException ignored) {
            failPending("Канал агента закрыт");
        }
    }

    private synchronized void write(JsonObject record) throws IOException {
        // Пишет одну строку JSONL
        OutputStream out = process.stdin();
        out.write((gson.toJson(record) + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void dispatch(String line, Consumer<JsonObject> events) {
        // Разделяет ответы и события
        JsonObject record;
        try {
            record = JsonParser.parseString(line).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            return;
        }
        if ("response".equals(JsonFields.text(record, "type"))) {
            complete(record);
        } else {
            events.accept(record);
        }
    }

    private void complete(JsonObject response) {
        // Завершает ожидающий запрос
        CompletableFuture<JsonObject> future = pending.remove(JsonFields.text(response, "id"));
        if (future == null) {
            return;
        }
        if (JsonFields.flag(response, "success")) {
            future.complete(JsonFields.object(response, "data"));
        } else {
            future.completeExceptionally(new RpcException(JsonFields.text(response, "error")));
        }
    }

    private void failPending(String reason) {
        // Отменяет все ожидающие запросы
        pending.values().forEach(future -> future.completeExceptionally(new RpcException(reason)));
        pending.clear();
    }
}
