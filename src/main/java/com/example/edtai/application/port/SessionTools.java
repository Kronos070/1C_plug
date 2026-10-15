package com.example.edtai.application.port;

import com.example.edtai.domain.SessionStats;
import com.example.edtai.domain.SlashCommand;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SessionTools {

    CompletableFuture<List<SlashCommand>> commands();

    CompletableFuture<String> compact(String instructions);

    CompletableFuture<Void> renameSession(String name);

    CompletableFuture<String> exportHtml();

    CompletableFuture<SessionStats> sessionStats();
}
