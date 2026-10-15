package com.example.edtai.bootstrap;

import com.example.edtai.application.ChatPorts;
import com.example.edtai.application.ChatService;
import com.example.edtai.application.port.ChatOutput;
import com.example.edtai.infrastructure.auth.AuthFileStore;
import com.example.edtai.infrastructure.files.WorkspaceFiles;
import com.example.edtai.infrastructure.marks.MarkerWriter;
import com.example.edtai.infrastructure.mock.MockAgentGateway;
import com.example.edtai.infrastructure.mock.MockCredentialStore;
import com.example.edtai.infrastructure.process.ExecutableLocator;
import com.example.edtai.infrastructure.process.LaunchSpecFactory;
import com.example.edtai.infrastructure.rpc.PerunGateway;
import com.example.edtai.infrastructure.session.FileSessionCatalog;
import com.example.edtai.infrastructure.settings.PluginSettings;
import com.example.edtai.infrastructure.settings.PreferencePermissionStore;
import com.example.edtai.infrastructure.settings.WorkspaceLocator;
import com.example.edtai.ui.marks.AnnotationSync;
import java.nio.file.Path;

public final class ChatAssembler {

    private ChatAssembler() {
    }

    public static ChatService assemble(PluginSettings settings, ChatOutput page) {
        // Собирает сервис чата по настройкам
        ChatOutput output = new DocsStatusRelay(page);
        Path root = WorkspaceLocator.resolve(settings.workDirectory());
        WorkspaceFiles files = new WorkspaceFiles(root);
        MarkerWriter marks = new MarkerWriter(AnnotationSync::refresh);
        if (settings.demoMode()) {
            MockAgentGateway mock = new MockAgentGateway();
            FeedbackAssembler.registerGateway(mock);
            return new ChatService(new ChatPorts(mock, mock.catalog(), new MockCredentialStore(), files,
                    new PreferencePermissionStore(), marks, FeedbackAssembler.rules()), output);
        }
        LaunchSpecFactory factory = new LaunchSpecFactory(new ExecutableLocator());
        PerunGateway gateway = new PerunGateway(() -> factory.create(settings.command(), settings.arguments(), root));
        FeedbackAssembler.registerGateway(gateway);
        return new ChatService(new ChatPorts(gateway, new FileSessionCatalog(), new AuthFileStore(), files,
                new PreferencePermissionStore(), marks, FeedbackAssembler.rules()), output);
    }
}
