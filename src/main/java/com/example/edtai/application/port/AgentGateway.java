package com.example.edtai.application.port;

import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.Message;
import com.example.edtai.domain.ModelInfo;
import com.example.edtai.domain.ThinkingLevel;
import com.example.edtai.domain.UiAnswer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AgentGateway {

    String name();

    void start(AgentListener listener) throws AgentUnavailableException;

    void stop();

    CompletableFuture<PromptOutcome> prompt(String text);

    CompletableFuture<Void> abort();

    CompletableFuture<AgentState> state();

    CompletableFuture<List<Message>> messages();

    CompletableFuture<Void> newSession();

    CompletableFuture<Void> openSession(String path);

    CompletableFuture<Void> selectModel(ModelInfo model);

    CompletableFuture<Void> selectThinking(ThinkingLevel level);

    CompletableFuture<Void> selectMode(AgentMode mode);

    void answer(UiAnswer answer);

    SessionTools tools();
}
