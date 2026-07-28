package com.anki.vocab_server.service.proxy;

public interface ProxyProvider {
    ProxySession acquire(ProxyRequest request);

    void release(ProxySession session);

    boolean refresh(ProxySession session);

    HealthStatus check(ProxySession session);
}
