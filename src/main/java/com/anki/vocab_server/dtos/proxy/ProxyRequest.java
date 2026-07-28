package com.anki.vocab_server.dtos.proxy;

import com.anki.vocab_server.model.Provider;

import java.util.UUID;

public record ProxyRequest(
        UUID tokenId,
        String idIsp,
        String idLocation,
        String token,
        Provider provider
) {
}
