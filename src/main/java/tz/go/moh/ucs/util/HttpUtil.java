package tz.go.moh.ucs.util;

import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import tz.go.moh.ucs.domain.HttpMethod;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;


public class HttpUtil {

    private HttpUtil() {

    }

    private static CloseableHttpClient init(String host) {
        try {

            HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            clientBuilder.setSSLContext(SSLContext.getDefault());
            clientBuilder.setRedirectStrategy(new DefaultRedirectStrategy());
            return clientBuilder.build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse post(String url, String payload, String data, String username, String password) {
        return post(url, payload, data, "application/json", AuthType.BASIC, username + ":" + password);
    }

    public static HttpResponse post(String url, String payload, String data) {
        return post(url, payload, data, "application/json", AuthType.NONE, "");
    }

    public static HttpResponse postWithToken(String url, String payload, String data, String token) {
        return post(url, payload, data, "application/json", AuthType.TOKEN, token);
    }

    public static HttpResponse post(String url, String payload, String data, String contentType, AuthType authType,
                                    String authString) {
        try {
            HttpPost request = (HttpPost) makeConnection(url, payload, HttpMethod.POST, authType, authString);
            request.setHeader(HTTP.CONTENT_TYPE, contentType);
            StringEntity entity = new StringEntity(data == null ? "" : data);
            System.out.println(data);
            entity.setContentEncoding(contentType);
            request.setEntity(entity);
            CloseableHttpClient httpClient = init(request.getURI().getHost());
            return httpClient.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse get(String url, String payload, String username, String password) {
        return get(url, payload, AuthType.BASIC, username + ":" + password);
    }

    public static HttpResponse delete(String url, String payload, String username, String password) {
        return delete(url, payload, AuthType.BASIC, username + ":" + password);
    }

    public static HttpResponse get(String url, String payload) {
        return get(url, payload, AuthType.NONE, "");
    }

    public static HttpResponse getWithToken(String url, String payload, String token) {
        return get(url, payload, AuthType.BASIC, token);
    }

    public static HttpResponse get(String url, String payload, AuthType authType, String authString) {
        try {
            HttpGet request = (HttpGet) makeConnection(url, payload, HttpMethod.GET, authType, authString);
            CloseableHttpClient httpClient = init(request.getURI().getHost());
            return httpClient.execute(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse delete(String url, String payload, AuthType authType, String authString) {
        try {
            HttpDelete request = (HttpDelete) makeConnection(url, payload, HttpMethod.DELETE, authType, authString);
            CloseableHttpClient httpClient = init(request.getURI().getHost());
            return httpClient.execute(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    static boolean checkSuccessBasedOnHttpCode(int httpCode) {
        if (httpCode >= 400 && httpCode <= 599) {
            return false;
        } else {
            return true;
        }
    }

    public static HttpRequestBase makeConnection(String baseUrl, String payload, HttpMethod method, AuthType authType,
                                                 String authString) throws URISyntaxException {
        String charset = "UTF-8";
        String url = baseUrl;
        if (url.endsWith("/")) {
            url = url.substring(0, url.lastIndexOf("/"));
        }
        url = (url + (StringUtils.isBlank(payload) ? "" : ("?" + payload))).replaceAll(" ", "%20");
        URI urlo = new URI(url);

        HttpRequestBase requestBase = null;
        if (method.equals(HttpMethod.GET)) {
            requestBase = new HttpGet(urlo);
        } else if (method.equals(HttpMethod.POST)) {
            requestBase = new HttpPost(urlo);
        } else if (method.equals(HttpMethod.PUT)) {
            requestBase = new HttpPut(urlo);
        } else if (method.equals(HttpMethod.DELETE)) {
            requestBase = new HttpDelete(urlo);
        }
        requestBase.setURI(urlo);
        requestBase.addHeader("Accept-Charset", charset);

        if (authType.name().equalsIgnoreCase("basic")) {
            String encoded = authString.matches(".+:.+") ? new String(Base64.encodeBase64(authString.getBytes()))
                    : authString;
            requestBase.addHeader("Authorization", "Basic " + encoded);
        } else if (authType.name().equalsIgnoreCase("token")) {
            requestBase.addHeader("Authorization", "Token " + authString);
        }

        System.out.println(url);
        return requestBase;
    }

    public static String removeEndingSlash(String str) {
        return str.endsWith("/") ? str.substring(0, str.lastIndexOf("/")) : str;
    }

    public static String removeTrailingSlash(String str) {
        return str.startsWith("/") ? str.substring(1) : str;
    }

    public enum AuthType {
        BASIC,
        TOKEN,
        NONE
    }

    public static String getURL(String url, String username, String password) throws IOException {
        Request request = new Request.Builder().url(url)
                .addHeader("Authorization", Credentials.basic(username, password)).build();
//        OkHttpClient client = new OkHttpClient();

        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .build();

        Call call = client.newCall(request);
        Response response;
        response = call.execute();
        String responseBody = response.body().string();
        if (!StringUtils.isBlank(responseBody)) {
            return responseBody;
        }
        return null;

    }
}
