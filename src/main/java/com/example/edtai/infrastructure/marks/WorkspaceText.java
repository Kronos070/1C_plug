package com.example.edtai.infrastructure.marks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public final class WorkspaceText {

    private WorkspaceText() {
    }

    public static Optional<IFile> fileFor(String path) {
        // Находит файл рабочей области по пути
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new File(path).toURI());
        for (IFile file : files) {
            if (file.exists()) {
                return Optional.of(file);
            }
        }
        for (IFile file : files) {
            if (file.getProject().isOpen()) {
                return Optional.of(file);
            }
        }
        return Optional.empty();
    }

    public static String read(IFile file) throws CoreException, IOException {
        // Читает текст файла без BOM
        try (InputStream in = file.getContents(true)) {
            String text = new String(in.readAllBytes(), Charset.forName(file.getCharset()));
            return text.startsWith("﻿") ? text.substring(1) : text;
        }
    }

    public static String slice(String text, int start, int end) {
        // Безопасно вырезает диапазон текста
        int from = Math.max(0, Math.min(start, text.length()));
        int to = Math.max(from, Math.min(end, text.length()));
        return text.substring(from, to);
    }
}
