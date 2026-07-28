package com.anki.vocab_server.service.proxy;

import java.time.Instant;

class ProxySession {

    String id;

    ProxyEndpoint endpoint;

    Instant expireAt;

    Capability capability;

}
