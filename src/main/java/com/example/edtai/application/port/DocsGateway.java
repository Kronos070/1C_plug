package com.example.edtai.application.port;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface DocsGateway {

    boolean supported();

    Path workDirectory();

    CompletableFuture<JsonObject> call(String method, JsonObject params);
}
