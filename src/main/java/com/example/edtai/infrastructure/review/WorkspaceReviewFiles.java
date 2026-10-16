package com.example.edtai.infrastructure.review;

import com.example.edtai.application.port.ReviewFiles;
import com.example.edtai.infrastructure.marks.WorkspaceText;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public final class WorkspaceReviewFiles implements ReviewFiles {

    @Override
    public Optional<String> read(String path) {
        // Читает файл с диска как есть
        Path file = Path.of(path);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public void write(String path, String text) throws IOException {
        // Записывает файл через рабочую область EDT
        Optional<IFile> file = WorkspaceText.fileFor(path);
        if (file.isEmpty()) {
            Files.writeString(Path.of(path), text, StandardCharsets.UTF_8);
            return;
        }
        try {
            IFile target = file.get();
            target.refreshLocal(IResource.DEPTH_ZERO, null);
            byte[] bytes = text.getBytes(charset(target));
            if (target.exists()) {
                target.setContents(new ByteArrayInputStream(bytes), IResource.FORCE | IResource.KEEP_HISTORY, null);
            } else {
                target.create(new ByteArrayInputStream(bytes), IResource.FORCE, null);
            }
        } catch (CoreException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(String path) throws IOException {
        // Удаляет созданный агентом файл
        Optional<IFile> file = WorkspaceText.fileFor(path);
        try {
            if (file.isPresent()) {
                file.get().refreshLocal(IResource.DEPTH_ZERO, null);
            }
            if (file.isPresent() && file.get().exists()) {
                file.get().delete(IResource.FORCE | IResource.KEEP_HISTORY, null);
            } else {
                Files.deleteIfExists(Path.of(path));
            }
        } catch (CoreException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static Charset charset(IFile file) {
        // Определяет кодировку файла рабочей области
        try {
            return Charset.forName(file.getCharset());
        } catch (CoreException | IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
