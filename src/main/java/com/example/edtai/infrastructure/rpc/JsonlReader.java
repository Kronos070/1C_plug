package com.example.edtai.infrastructure.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class JsonlReader {

    private JsonlReader() {
    }

    public static Thread start(InputStream input, Consumer<String> onLine, Runnable onEnd) {
        // Читает строки JSONL в фоне
        Thread thread = new Thread(() -> read(input, onLine, onEnd), "perun-stdout");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void read(InputStream input, Consumer<String> onLine, Runnable onEnd) {
        // Делит поток строго по LF
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        try (InputStream in = input) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == '\n') {
                        emit(line, onLine);
                    } else {
                        line.write(buffer[i]);
                    }
                }
            }
            emit(line, onLine);
        } catch (IOException ignored) {
            emit(line, onLine);
        } finally {
            onEnd.run();
        }
    }

    private static void emit(ByteArrayOutputStream line, Consumer<String> onLine) {
        // Отдает накопленную строку дальше
        String text = line.toString(StandardCharsets.UTF_8);
        line.reset();
        if (text.endsWith("\r")) {
            text = text.substring(0, text.length() - 1);
        }
        if (!text.isBlank()) {
            onLine.accept(text);
        }
    }
}
