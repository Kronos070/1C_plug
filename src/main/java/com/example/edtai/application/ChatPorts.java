package com.example.edtai.application;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.AiCodeMarks;
import com.example.edtai.application.port.CredentialStore;
import com.example.edtai.application.port.FileFinder;
import com.example.edtai.application.port.PermissionStore;
import com.example.edtai.application.port.RuleStore;
import com.example.edtai.application.port.SessionCatalog;

public record ChatPorts(
        AgentGateway gateway,
        SessionCatalog catalog,
        CredentialStore credentials,
        FileFinder files,
        PermissionStore permissions,
        AiCodeMarks marks,
        RuleStore rules) {
}
