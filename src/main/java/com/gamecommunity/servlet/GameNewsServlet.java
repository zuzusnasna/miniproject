package com.gamecommunity.servlet;

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

/**
 * 네이버 뉴스 API를 이용해 게임 관련 뉴스를 가져오는 Servlet입니다.
 *
 * 요청 흐름
 * 1. 응답 형식 설정
 * 2. 캐시된 뉴스가 있는지 확인
 * 3. 캐시가 유효하면 기존 데이터를 반환
 * 4. 캐시가 없거나 만료되면 네이버 API 호출
 * 5. 조회 결과를 캐시에 저장
 * 6. 뉴스 JSON 반환
 *
 * API 인증 정보는 코드에 직접 작성하지 않고
 * resources/naver-api.properties에서 읽어옵니다.
 */
@WebServlet("/game-news")
public class GameNewsServlet extends HttpServlet {

    // 뉴스 데이터를 30분 동안 캐시합니다.
    private static final long CACHE_DURATION_MS =
            30L * 60L * 1000L;

    // 여러 요청이 동시에 뉴스 API를 호출하지 않도록 잠금 객체를 사용합니다.
    private static final Object CACHE_LOCK = new Object();

    // 마지막으로 가져온 뉴스 JSON을 저장합니다.
    private static volatile String cachedNews;

    // 뉴스 API를 마지막으로 호출한 시간을 저장합니다.
    private static volatile long lastFetchedTime;

    // 외부 뉴스 API와 통신하는 HTTP 클라이언트입니다.
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 게임 뉴스를 조회합니다.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // =====================================================
        // 1. 응답 설정
        // =====================================================

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        // =====================================================
        // 2. 캐시 확인
        // =====================================================

        long now = System.currentTimeMillis();
        String currentCache = cachedNews;

        // 캐시가 존재하고 아직 30분이 지나지 않았다면 API를 호출하지 않습니다.
        if (isCacheValid(currentCache, now)) {
            response.setHeader("X-GameHub-News-Cache", "HIT");
            response.getWriter().write(currentCache);
            return;
        }

        // =====================================================
        // 3. 뉴스 API 호출
        // =====================================================

        synchronized (CACHE_LOCK) {

            // 다른 요청이 먼저 API를 호출했을 수 있으므로 캐시를 다시 확인합니다.
            now = System.currentTimeMillis();
            currentCache = cachedNews;

            if (isCacheValid(currentCache, now)) {
                response.setHeader("X-GameHub-News-Cache", "HIT");
                response.getWriter().write(currentCache);
                return;
            }

            try {
                // API 인증 정보를 properties 파일에서 읽습니다.
                ApiCredentials credentials = loadCredentials();

                // 네이버 뉴스 API를 호출합니다.
                String newsJson = fetchNews(credentials);

                // 정상적으로 가져온 뉴스는 캐시에 저장합니다.
                cachedNews = newsJson;
                lastFetchedTime = System.currentTimeMillis();

                response.setHeader("X-GameHub-News-Cache", "MISS");
                response.getWriter().write(newsJson);

            } catch (Exception e) {

                // API 호출에 실패하더라도 이전에 저장된 뉴스가 있으면 사용합니다.
                if (cachedNews != null) {
                    response.setHeader("X-GameHub-News-Cache", "STALE");
                    response.getWriter().write(cachedNews);
                    return;
                }

                // 사용할 수 있는 캐시도 없다면 서비스 이용 불가 응답을 반환합니다.
                response.setStatus(
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE
                );

                response.getWriter().write(
                        "{\"error\":\"NEWS_UNAVAILABLE\","
                                + "\"message\":\"게임 뉴스를 불러올 수 없습니다.\"}"
                );
            }
        }
    }

    /**
     * 현재 캐시가 사용할 수 있는 상태인지 확인합니다.
     */
    private boolean isCacheValid(
            String cache,
            long currentTime
    ) {

        return cache != null
                && currentTime - lastFetchedTime < CACHE_DURATION_MS;
    }

    /**
     * 네이버 API 인증 정보를 properties 파일에서 읽습니다.
     */
    private ApiCredentials loadCredentials() throws IOException {

        Properties properties = new Properties();

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("naver-api.properties")) {

            if (input == null) {
                throw new IOException(
                        "naver-api.properties 파일을 찾을 수 없습니다."
                );
            }

            properties.load(input);
        }

        String clientId = properties.getProperty("naver.client.id");
        String clientSecret = properties.getProperty("naver.client.secret");

        // API 인증 정보가 없으면 호출하지 않습니다.
        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {

            throw new IOException(
                    "NAVER API Client ID 또는 Client Secret이 설정되지 않았습니다."
            );
        }

        return new ApiCredentials(
                clientId.trim(),
                clientSecret.trim()
        );
    }

    /**
     * 네이버 뉴스 API를 호출합니다.
     */
    private String fetchNews(
            ApiCredentials credentials
    ) throws IOException, InterruptedException {

        // 검색어를 URL에 넣을 수 있는 형식으로 변환합니다.
        String query = URLEncoder.encode(
                "게임",
                StandardCharsets.UTF_8
        );

        // 뉴스 API 요청 주소를 구성합니다.
        String apiUrl = "https://naverapihub.apigw.ntruss.com/search/v1/news"
                + "?query=" + query
                + "&display=5"
                + "&start=1"
                + "&sort=date"
                + "&format=json";

        // 네이버 뉴스 API 요청을 생성합니다.
        HttpRequest apiRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(8))
                .header(
                        "X-NCP-APIGW-API-KEY-ID",
                        credentials.clientId()
                )
                .header(
                        "X-NCP-APIGW-API-KEY",
                        credentials.clientSecret()
                )
                .GET()
                .build();

        // 실제 API 요청을 보내고 응답을 받습니다.
        HttpResponse<String> apiResponse = httpClient.send(
                apiRequest,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        // 200번대 응답이 아니면 API 호출 실패로 처리합니다.
        if (apiResponse.statusCode() < 200
                || apiResponse.statusCode() >= 300) {

            throw new IOException(
                    "NAVER 뉴스 API 호출 실패: HTTP "
                            + apiResponse.statusCode()
            );
        }

        return apiResponse.body();
    }

    /**
     * 네이버 API 인증 정보를 하나의 객체로 관리합니다.
     */
    private record ApiCredentials(
            String clientId,
            String clientSecret
    ) {
    }
}
