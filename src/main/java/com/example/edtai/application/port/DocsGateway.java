package com.example.edtai.application.port;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;

public interface DocsGateway {

    boolean supported();

    CompletableFuture<JsonObject> call(String method, JsonObject params);
}
