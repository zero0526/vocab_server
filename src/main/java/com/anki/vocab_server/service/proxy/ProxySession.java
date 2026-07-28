package com.anki.vocab_server.service.proxy;

import com.anki.vocab_server.dtos.proxy.Proxy;
import com.anki.vocab_server.dtos.proxy.ProxyRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProxySession {

    UUID id;

    Proxy proxy;

    Instant nextReq;

    ProxyProvider provider;
    ProxyRequest request;
}
