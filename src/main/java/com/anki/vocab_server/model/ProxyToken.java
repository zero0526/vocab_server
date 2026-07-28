package com.anki.vocab_server.model;

import com.anki.vocab_server.enums.ProxyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("proxy_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyToken {
    private UUID id;
    private String token;
    private Integer nextReq;
    private LocalDateTime expiredAt;
    private int maxIpPerDay;
    private int ipUsedToday;
    private ProxyType proxyType;
    private Provider provider;
    private boolean enable;
    private String reason;
}
