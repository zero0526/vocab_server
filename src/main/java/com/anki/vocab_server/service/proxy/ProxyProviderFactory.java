package com.anki.vocab_server.service.proxy;

import com.anki.vocab_server.model.Provider;
import com.anki.vocab_server.service.proxy.provider.TMProxyProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProxyProviderFactory {

    private final Map<String, ProxyProvider> providers;

    public ProxyProviderFactory(List<ProxyProvider> providerList) {
        // Tự động gom các Spring bean triển khai ProxyProvider theo convention tên class hoặc custom name
        this.providers = providerList.stream().collect(Collectors.toMap(
                p -> p.getClass().getSimpleName().toLowerCase(),
                Function.identity()
        ));
    }

    public ProxyProvider getProvider(Provider provider) {
        if (provider == null || provider.getName() == null) {
            throw new IllegalArgumentException("Provider or provider name cannot be null");
        }

        // Tìm theo provider.getName() (VD: "TMProxy" -> tmproxyprovider)
        String keyName = (provider.getName() + "Provider").toLowerCase();
        
        return Optional.ofNullable(providers.get(keyName))
                .orElseGet(() -> Optional.ofNullable(providers.get(provider.getName().toLowerCase()))
                        .orElseThrow(() -> new IllegalArgumentException("Unsupported proxy provider: " + provider.getName())));
    }
}
