package com.example.edtai.infrastructure.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class AgentProcess {

    private static final int STDERR_LIMIT = 4000;

    private final Process process;
    private final StringBuilder stderr = new StringBuilder();

    private AgentProcess(Process process) {
        this.process = process;
    }

    public static AgentProcess start(LaunchSpec spec) throws IOException {
        // Запускает процесс агента
        ProcessBuilder builder = new ProcessBuilder(spec.command());
        builder.directory(spec.workDirectory().toFile());
        builder.environment().putAll(spec.environment());
        AgentProcess agent = new AgentProcess(builder.start());
        agent.drainStderr();
        return agent;
    }

    public InputStream stdout() {
        // Возвращает поток вывода агента
        return process.getInputStream();
    }

    public OutputStream stdin() {
        // Возвращает поток ввода агента
        return process.getOutputStream();
    }

    public boolean isAlive() {
        // Проверяет, что процесс работает
        return process.isAlive();
    }

    public String stderrTail() {
        // Возвращает хвост журнала ошибок
        synchronized (stderr) {
            return stderr.toString().strip();
        }
    }

    public void destroy() {
        // Корректно завершает процесс агента
        try {
            process.getOutputStream().close();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            }
        } catch (IOException e) {
            process.destroyForcibly();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private void drainStderr() {
        // Читает журнал ошибок в фоне
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            try (InputStream in = process.getErrorStream()) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {
                return;
            }
        }, "perun-stderr");
        thread.setDaemon(true);
        thread.start();
    }

    private void append(String chunk) {
        // Дописывает журнал с ограничением
        synchronized (stderr) {
            stderr.append(chunk);
            if (stderr.length() > STDERR_LIMIT) {
                stderr.delete(0, stderr.length() - STDERR_LIMIT);
            }
        }
    }
}
