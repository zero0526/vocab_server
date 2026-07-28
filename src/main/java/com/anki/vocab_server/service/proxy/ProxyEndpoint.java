package com.anki.vocab_server.service.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProxyEndpoint {
    private String host;
    private String port;
    private String userName;
    private String password;
}
