package com.anki.vocab_server.service.proxy;

import com.anki.vocab_server.dtos.proxy.Proxy;
import com.anki.vocab_server.dtos.proxy.ProxyRequest;
import com.anki.vocab_server.model.ProxyToken;
import com.anki.vocab_server.repository.ProxyTokenRepository;
import com.anki.vocab_server.service.proxy.provider.TMProxyProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyManager {
    private final ProxyTokenRepository proxyTokenRepository;
    private final ProxyProviderFactory proxyProviderFactory;
    private final ConcurrentHashMap<UUID, ProxySession> activeSessions = new ConcurrentHashMap<>();
    private final Deque<UUID> sessionQueue = new LinkedList<>();
    private int maxSessionPool = 10;

    @PostConstruct
    public synchronized void initPool() {
        log.info("Initializing Proxy Pool...");
        List<ProxyToken> tokenList = new ArrayList<>();
        proxyTokenRepository.findByEnableTrue().forEach(tokenList::add);

        // Xáo trộn danh sách để lấy ngẫu nhiên không theo thứ tự
        Collections.shuffle(tokenList);

        for (ProxyToken token : tokenList) {
            if (activeSessions.size() >= maxSessionPool) {
                log.info("Proxy pool has reached max capacity of {}", maxSessionPool);
                break;
            }

            try {
                ProxyProvider provider = proxyProviderFactory.getProvider(token.getProvider());
                ProxyRequest proxyRequest = provider.checkStats(token);

                // Kiểm tra xem đã vượt quá số lần đổi IP trong ngày hay chưa
                if (token.getIpUsedToday() >= token.getMaxIpPerDay() && token.getMaxIpPerDay() > 0) {
                    log.warn("ProxyToken {} disabled due to exceeding daily IP limit", token.getId());
                    invalidateToken(token.getId(), "Exceeded daily IP change limit during initialization");
                    continue;
                }

                // Kiểm tra nếu hết hạn (expiredAt) -> disable trong DB và bỏ qua
                if (token.getExpiredAt() != null && token.getExpiredAt().isBefore(java.time.LocalDateTime.now())) {
                    log.warn("ProxyToken {} disabled due to expiration", token.getId());
                    invalidateToken(token.getId(), "Token expired");
                    continue;
                }

                // Khởi tạo proxy session và lưu vào pool
                ProxySession session = provider.acquire(proxyRequest);
                activeSessions.put(token.getId(), session);
                sessionQueue.addLast(token.getId());
                log.info("Successfully loaded ProxyToken {} into pool", token.getId());
            } catch (Exception e) {
                log.error("Failed to initialize ProxyToken {}: {}", token.getId(), e.getMessage());
                // Nêu gặp lỗi kiểm tra stat hoặc không kết nối được -> disable token và lưu lý do
                invalidateToken(token.getId(), "Initialization failed: " + e.getMessage());
            }
        }
    }

    public synchronized Proxy acquire(UUID tokenId) throws IOException {
        // 1. Kiểm tra trong active sessions trước
        if (activeSessions.containsKey(tokenId)) {
            ProxySession session = activeSessions.get(tokenId);
            // Re-order queue để tokenId này lên đầu (gần đây nhất)
            sessionQueue.remove(tokenId);
            sessionQueue.addFirst(tokenId);
            return session.getProxy();
        }

        // 2. Kéo ProxyToken từ DB lên
        ProxyToken proxyToken = proxyTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("ProxyToken not found with id: " + tokenId));

        if (!proxyToken.isEnable()) {
            throw new IllegalStateException("ProxyToken is disabled: " + tokenId);
        }

        // 3. Gọi providerFactory để lấy ProxyProvider đa hình dựa trên Provider trong DB
        ProxyProvider provider = proxyProviderFactory.getProvider(proxyToken.getProvider());
        ProxyRequest proxyRequest = provider.checkStats(proxyToken);

        // 4. Kiểm tra xem đã vượt quá số lần đổi IP trong ngày hay chưa
        if (proxyToken.getIpUsedToday() >= proxyToken.getMaxIpPerDay() && proxyToken.getMaxIpPerDay() > 0) {
            throw new IllegalStateException("ProxyToken " + tokenId + " has exceeded daily IP change limit.");
        }

        // 5. Nếu pool đã đầy -> Đẩy session ở cuối queue đi
        if (sessionQueue.size() >= maxSessionPool && !sessionQueue.isEmpty()) {
            UUID evictedTokenId = sessionQueue.removeLast();
            activeSessions.remove(evictedTokenId);
        }

        // 6. Khởi tạo session mới và lưu vào pool (đưa lên đầu queue)
        ProxySession newSession = provider.acquire(proxyRequest);
        activeSessions.put(tokenId, newSession);
        sessionQueue.addFirst(tokenId);

        return newSession.getProxy();
    }

    public synchronized void release(UUID tokenId) {
        if (tokenId != null) {
            activeSessions.remove(tokenId);
            sessionQueue.remove(tokenId);
        }
    }


    public synchronized void removeSession(UUID tokenId) {
        release(tokenId);
    }

    public synchronized void invalidateToken(UUID tokenId, String reason) {
        // 1. Xóa khỏi memory pool
        release(tokenId);

        // 2. Disable trong DB và lưu lý do
        proxyTokenRepository.findById(tokenId).ifPresent(proxyToken -> {
            proxyToken.setEnable(false);
            proxyToken.setReason(reason);
            proxyTokenRepository.save(proxyToken);
        });
    }

    public synchronized Proxy acquireRandom() {
        if (activeSessions.isEmpty()) {
            initPool();
        }
        if (activeSessions.isEmpty()) {
            return null;
        }
        List<ProxySession> sessions = new ArrayList<>(activeSessions.values());
        int randomIndex = ThreadLocalRandom.current().nextInt(sessions.size());
        return sessions.get(randomIndex).getProxy();
    }

    public synchronized void refreshPool() {
        log.info("Refreshing Proxy Pool...");
        activeSessions.clear();
        sessionQueue.clear();
    }
}
