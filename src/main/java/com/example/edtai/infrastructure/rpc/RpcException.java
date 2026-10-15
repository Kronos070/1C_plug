package com.example.edtai.infrastructure.rpc;

public final class RpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RpcException(String message) {
        super(message);
    }
}
