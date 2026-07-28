package com.anki.vocab_server.utils;

import com.anki.vocab_server.dtos.proxy.Proxy;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpProxyUtils {

    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();

    public static Response executeRequest(Request request, Proxy proxy) throws IOException {
        OkHttpClient client = createClient(proxy);
        return client.newCall(request).execute();
    }

    public static OkHttpClient createClient(Proxy proxy) {
        if (proxy == null || proxy.getHost() == null || proxy.getHost().isEmpty()) {
            return DEFAULT_CLIENT;
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(new java.net.Proxy(
                        java.net.Proxy.Type.HTTP,
                        new InetSocketAddress(proxy.getHost(), proxy.getPort())
                ));

        // Nêu proxy có yêu cầu User/Pass authentication
        if (proxy.getUsername() != null && !proxy.getUsername().isEmpty() &&
            proxy.getPassword() != null && !proxy.getPassword().isEmpty()) {

            Authenticator proxyAuthenticator = (route, response) -> {
                String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            };
            builder.proxyAuthenticator(proxyAuthenticator);
        }

        return builder.build();
    }
}
