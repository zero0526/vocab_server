package com.anki.vocab_server.service.proxy;

import com.anki.vocab_server.dtos.proxy.ProxyRequest;
import com.anki.vocab_server.model.ProxyToken;

import java.io.IOException;

public interface ProxyProvider {

    ProxySession acquire(ProxyRequest request) throws IOException;

    ProxySession rotate(ProxySession session) throws  IOException;

    ProxyRequest checkStats(ProxyToken session) throws IOException;
}
