/**
 *
 */
package tz.go.moh.ucs.util;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * @author Samuel Githengi created on 09/10/20
 */
public class HttpUtils {


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
