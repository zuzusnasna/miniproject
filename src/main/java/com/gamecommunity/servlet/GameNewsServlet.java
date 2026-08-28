package com.gamecommunity.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

@WebServlet("/game-news")
public class GameNewsServlet extends HttpServlet {

    private static final long CACHE_DURATION_MS = 30L * 60L * 1000L;
    private static final Object CACHE_LOCK = new Object();

    private static volatile String cachedNews;
    private static volatile long lastFetchedTime;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        long now = System.currentTimeMillis();
        String currentCache = cachedNews;

        if (currentCache != null && now - lastFetchedTime < CACHE_DURATION_MS) {
            response.setHeader("X-GameHub-News-Cache", "HIT");
            response.getWriter().write(currentCache);
            return;
        }

        synchronized (CACHE_LOCK) {
            now = System.currentTimeMillis();
            currentCache = cachedNews;

            if (currentCache != null && now - lastFetchedTime < CACHE_DURATION_MS) {
                response.setHeader("X-GameHub-News-Cache", "HIT");
                response.getWriter().write(currentCache);
                return;
            }

            try {
                ApiCredentials credentials = loadCredentials();
                String newsJson = fetchNews(credentials);

                cachedNews = newsJson;
                lastFetchedTime = System.currentTimeMillis();

                response.setHeader("X-GameHub-News-Cache", "MISS");
                response.getWriter().write(newsJson);
            } catch (Exception e) {
                e.printStackTrace();

                if (cachedNews != null) {
                    response.setHeader("X-GameHub-News-Cache", "STALE");
                    response.getWriter().write(cachedNews);
                    return;
                }

                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getWriter().write("{\"error\":\"NEWS_UNAVAILABLE\",\"message\":\"게임 뉴스를 불러올 수 없습니다.\"}");
            }
        }
    }

    private ApiCredentials loadCredentials() throws IOException {
        Properties properties = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("naver-api.properties")) {
            if (input == null) {
                throw new IOException("naver-api.properties 파일을 찾을 수 없습니다.");
            }
            properties.load(input);
        }

        String clientId = properties.getProperty("naver.client.id");
        String clientSecret = properties.getProperty("naver.client.secret");

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IOException("NAVER API Client ID 또는 Client Secret이 설정되지 않았습니다.");
        }

        return new ApiCredentials(clientId.trim(), clientSecret.trim());
    }

    private String fetchNews(ApiCredentials credentials) throws IOException, InterruptedException {
        String query = URLEncoder.encode("게임", StandardCharsets.UTF_8);
        String apiUrl = "https://naverapihub.apigw.ntruss.com/search/v1/news"
                + "?query=" + query
                + "&display=5"
                + "&start=1"
                + "&sort=date"
                + "&format=json";

        HttpRequest apiRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(8))
                .header("X-NCP-APIGW-API-KEY-ID", credentials.clientId())
                .header("X-NCP-APIGW-API-KEY", credentials.clientSecret())
                .GET()
                .build();

        HttpResponse<String> apiResponse = httpClient.send(
                apiRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (apiResponse.statusCode() < 200 || apiResponse.statusCode() >= 300) {
            throw new IOException("NAVER 뉴스 API 호출 실패: HTTP " + apiResponse.statusCode());
        }

        return apiResponse.body();
    }

    private record ApiCredentials(String clientId, String clientSecret) {
    }
}
