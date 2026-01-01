package io.instana.slo.data.api;

import android.content.Context;

import io.instana.slo.util.PreferencesManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API client factory for creating Retrofit instances
 */
public class ApiClient {
    private static InstanaApiService apiService;
    private static Retrofit retrofit;

    /**
     * Get or create the API service instance
     * 
     * @param context Application context
     * @return InstanaApiService instance
     */
    public static InstanaApiService getApiService(Context context) {
        if (apiService == null) {
            PreferencesManager prefsManager = new PreferencesManager(context);
            String baseUrl = prefsManager.getApiEndpoint();
            String apiToken = prefsManager.getApiToken();

            retrofit = createRetrofit(baseUrl, apiToken);
            apiService = retrofit.create(InstanaApiService.class);
        }
        return apiService;
    }

    /**
     * Reset the API service (useful when settings change)
     */
    public static void resetApiService() {
        apiService = null;
        retrofit = null;
    }

    /**
     * Create a Retrofit instance with proper configuration
     * 
     * @param baseUrl The base URL for the API
     * @param apiToken The API token for authentication
     * @return Configured Retrofit instance
     */
    private static Retrofit createRetrofit(String baseUrl, String apiToken) {
        // Ensure base URL ends with /
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        OkHttpClient okHttpClient = createOkHttpClient(apiToken);

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Create OkHttpClient with interceptors for authentication and logging
     * 
     * @param apiToken The API token for authentication
     * @return Configured OkHttpClient
     */
    private static OkHttpClient createOkHttpClient(String apiToken) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // Add authentication interceptor
        builder.addInterceptor(new AuthInterceptor(apiToken));

        // Add logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(loggingInterceptor);

        return builder.build();
    }

    /**
     * Interceptor to add authentication header to all requests
     */
    private static class AuthInterceptor implements Interceptor {
        private final String apiToken;

        public AuthInterceptor(String apiToken) {
            this.apiToken = apiToken;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();

            Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", "apiToken " + apiToken)
                    .method(original.method(), original.body());

            Request request = requestBuilder.build();
            return chain.proceed(request);
        }
    }
}
