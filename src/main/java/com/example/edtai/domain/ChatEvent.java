package com.example.edtai.domain;

import java.util.List;

public sealed interface ChatEvent {

    record Connecting(String agent) implements ChatEvent {
    }

    record Connected(String agent) implements ChatEvent {
    }

    record Disconnected(String reason) implements ChatEvent {
    }

    record StateChanged(AgentState state) implements ChatEvent {
    }

    record HistoryLoaded(List<Message> messages) implements ChatEvent {
    }

    record SessionsListed(List<SessionSummary> sessions, String current) implements ChatEvent {
    }

    record CommandsListed(List<SlashCommand> commands) implements ChatEvent {
    }

    record FilesFound(String query, List<String> paths) implements ChatEvent {
    }

    record CodeWritten(String path, List<String> snippets, List<LineRange> ranges) implements ChatEvent {

        public CodeWritten {
            snippets = snippets == null ? List.of() : List.copyOf(snippets);
            ranges = ranges == null ? List.of() : List.copyOf(ranges);
        }

        public CodeWritten(String path, List<String> snippets) {
            this(path, snippets, List.of());
        }
    }

    record Working() implements ChatEvent {
    }

    record AssistantStarted() implements ChatEvent {
    }

    record TextDelta(int index, String delta) implements ChatEvent {
    }

    record ThinkingDelta(int index, String delta) implements ChatEvent {
    }

    record ToolStarted(String id, String name, String arguments) implements ChatEvent {
    }

    record ToolFinished(String id, String output, boolean failed) implements ChatEvent {
    }

    record AssistantFinished(Message message, String error) implements ChatEvent {
    }

    record Settled(boolean aborted) implements ChatEvent {
    }

    record Failure(String message) implements ChatEvent {
    }

    record Notice(String level, String text) implements ChatEvent {
    }

    record StatusChanged(String key, String text) implements ChatEvent {
    }

    record InputRequested(UiRequest request) implements ChatEvent {
    }

    record EditorTextSet(String text) implements ChatEvent {
    }
}
