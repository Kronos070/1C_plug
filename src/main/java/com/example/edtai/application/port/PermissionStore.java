package com.example.edtai.application.port;

import java.util.Set;

public interface PermissionStore {

    Set<String> allowed();

    Set<String> denied();

    void remember(String key, boolean allow);
}
