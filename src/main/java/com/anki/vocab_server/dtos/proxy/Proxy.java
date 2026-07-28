package com.anki.vocab_server.dtos.proxy;

import com.anki.vocab_server.enums.ProxyType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Proxy {

    String host;

    int port;

    String username;

    String password;
}