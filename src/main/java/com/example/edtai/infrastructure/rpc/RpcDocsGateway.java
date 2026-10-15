package com.example.edtai.infrastructure.rpc;

import com.example.edtai.application.port.AgentUnavailableException;
import com.example.edtai.application.port.DocsGateway;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class RpcDocsGateway implements DocsGateway {

    private static final String LONG_METHOD = "docs.sync";

    private final Supplier<RpcConnection> rpc;
    private volatile boolean supported;

    RpcDocsGateway(Supplier<RpcConnection> rpc) {
        this.rpc = rpc;
    }

    void support(boolean value) {
        // Запоминает поддержку документации агентом
        supported = value;
    }

    @Override
    public boolean supported() {
        // Сообщает, есть ли команда docs у агента
        return supported;
    }

    @Override
    public CompletableFuture<JsonObject> call(String method, JsonObject params) {
        // Вызывает метод через extension_request
        RpcConnection connection = rpc.get();
        if (connection == null) {
            return CompletableFuture.failedFuture(new AgentUnavailableException("ПерУН не запущен"));
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("method", method);
        payload.add("params", params == null ? new JsonObject() : params);
        return connection.request("extension_request", payload, !LONG_METHOD.equals(method));
    }
}
