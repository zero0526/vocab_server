package com.anki.vocab_server.service.proxy.provider;

import com.anki.vocab_server.dtos.proxy.Proxy;
import com.anki.vocab_server.dtos.proxy.ProxyRequest;
import com.anki.vocab_server.model.Provider;
import com.anki.vocab_server.model.ProxyToken;
import com.anki.vocab_server.service.proxy.ProxyProvider;
import com.anki.vocab_server.service.proxy.ProxySession;
import com.anki.vocab_server.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class TMProxyProvider implements ProxyProvider {
    private final OkHttpClient client = new OkHttpClient();

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @Override
    public ProxySession rotate(ProxySession session)throws IOException {
        ProxyRequest proxyRequest=session.getRequest();
        Map<String, Object> body = Map.of(
                "api_key", proxyRequest.token(),
                "id_location", proxyRequest.idLocation(),
                "id_isp", proxyRequest.idIsp()
        );
        TMProxyResponse tmResp = postApi(proxyRequest.provider(), "get_new", body, TMProxyResponse.class);
        return buildProxySession(proxyRequest, tmResp);
    }

    @Override
    public ProxyRequest checkStats(ProxyToken proxyToken)throws IOException {
        Map<String, Object> body = Map.of(
                "api_key", proxyToken.getToken()
        );
        TMProxyStatusResponse tmStat= postApi(proxyToken.getProvider(), "stat" ,body, TMProxyStatusResponse.class);
        StatsData data= tmStat.data;
        return new ProxyRequest(proxyToken.getId(), data.idIsp, data.idLocation, proxyToken.getToken(), proxyToken.getProvider());
    }

    @Override
    public ProxySession acquire(ProxyRequest proxyRequest) throws IOException {
        Map<String, Object> body = Map.of(
                "api_key", proxyRequest.token()
        );

        TMProxyResponse tmResp = postApi(proxyRequest.provider(), "get_current", body, TMProxyResponse.class);
        return buildProxySession(proxyRequest, tmResp);
    }

    private <T> T postApi(Provider provider, String endpointKey, Map<String, Object> body, Class<T> clazz) throws IOException {
        String endpoint = provider.getEndpoint().get(endpointKey);
        String url = provider.getBaseUrl() + endpoint;

        RequestBody requestBody = RequestBody.create(
                JsonUtils.mapToJson(body),
                JSON_MEDIA_TYPE
        );

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP error " + response.code() + " for URL: " + url);
            }
            if (response.body() == null) {
                throw new IOException("Empty response body from URL: " + url);
            }
            String json = response.body().string();
            T result = JsonUtils.fromJson(json, clazz);
            if (result == null) {
                throw new IOException("Failed to parse response JSON: " + json);
            }
            return result;
        }
    }

    private ProxySession buildProxySession(ProxyRequest proxyRequest, TMProxyResponse tmResp) {
        if (tmResp.data == null || tmResp.data.https == null) {
            throw new IllegalStateException("API returned no proxy data or invalid format. Message: " + tmResp.message);
        }

        Data data = tmResp.data;
        String[] urlComp = data.https.split(":");
        if (urlComp.length != 2) {
            throw new IllegalArgumentException("Url is incorrect format: " + data.https);
        }

        return ProxySession.builder()
                .id(proxyRequest.tokenId())
                .nextReq(data.nextReq != null ? Instant.ofEpochSecond(data.nextReq) : null)
                .proxy(new Proxy(urlComp[0], Integer.parseInt(urlComp[1]), data.username, data.password))
                .provider(this)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TMProxyResponse {
        public int code;
        public String message;
        public Data data;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TMProxyStatusResponse {
        public int code;
        public String message;
        public StatsData data;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public String username;
        public String password;

        @JsonProperty("public_ip")
        public String publicIp;

        @JsonProperty("isp_name")
        public String ispName;
        @JsonProperty("next_request")
        public Integer nextReq;
        public String https;

        public int timeout;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatsData {
        @JsonProperty("id_isp")
        public String idIsp;
        @JsonProperty("id_location")
        public String idLocation;
        @JsonProperty("expired_at")
        public String expiredAt;
        public Integer maxIpPerDay;
        public Integer ipUsedToday;
    }
}
