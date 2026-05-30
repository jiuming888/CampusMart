package com.campusmart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public List<SearchResult> search(String query, int maxResults) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Try Bing international first (better structured results)
            List<SearchResult> results = searchBing(encoded, maxResults);
            if (results.size() >= 2) return results;

            // Fallback to Baidu
            results.addAll(searchBaidu(encoded, maxResults - results.size()));
            return results.subList(0, Math.min(results.size(), maxResults));

        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return List.of();
        }
    }

    private List<SearchResult> searchBing(String encodedQuery, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String url = "https://www.bing.com/search?q=" + encodedQuery + "&setlang=zh-Hans&count=5";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            if (html == null || html.isEmpty()) return results;

            // Extract from <li class="b_algo"> blocks
            // Each result has <h2><a>title</a></h2> and <div class="b_caption"><p>snippet</p></div>
            Pattern blockPattern = Pattern.compile("<li class=\"b_algo\">(.*?)</li>", Pattern.DOTALL);
            Pattern titlePattern = Pattern.compile("<h2><a[^>]*>(.*?)</a></h2>", Pattern.DOTALL);
            Pattern snippetPattern = Pattern.compile("<p>(.*?)</p>", Pattern.DOTALL);
            Pattern tagPattern = Pattern.compile("<[^>]+>");

            Matcher blockMatcher = blockPattern.matcher(html);
            while (blockMatcher.find() && results.size() < maxResults) {
                String block = blockMatcher.group(1);

                String title = "";
                Matcher tm = titlePattern.matcher(block);
                if (tm.find()) {
                    title = tagPattern.matcher(tm.group(1)).replaceAll("").trim();
                }

                String snippet = "";
                Matcher sm = snippetPattern.matcher(block);
                if (sm.find()) {
                    snippet = decodeHtml(tagPattern.matcher(sm.group(1)).replaceAll("").trim());
                }

                if (!snippet.isEmpty() && snippet.length() > 15) {
                    results.add(new SearchResult(decodeHtml(title), snippet));
                }
            }

            log.info("Bing search found {} results", results.size());
        } catch (Exception e) {
            log.warn("Bing search failed: {}", e.getMessage());
        }
        return results;
    }

    private List<SearchResult> searchBaidu(String encodedQuery, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String url = "https://www.baidu.com/s?wd=" + encodedQuery + "&rn=5";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            if (html == null || html.isEmpty()) return results;

            // Extract from <div class="result"> or <div class="c-container">
            Pattern containerPattern = Pattern.compile("<div[^>]*class=\"[^\"]*(?:result|c-container)[^\"]*\"[^>]*>(.*?)</div>\\s*<div", Pattern.DOTALL);
            Pattern titlePattern = Pattern.compile("<h3[^>]*>(.*?)</h3>", Pattern.DOTALL);
            Pattern snippetPattern = Pattern.compile("<span class=\"content-right_[^\"]*\">(.*?)</span>|<div class=\"c-abstract[^\"]*\">(.*?)</div>", Pattern.DOTALL);
            Pattern tagPattern = Pattern.compile("<[^>]+>");

            Matcher containerMatcher = containerPattern.matcher(html);
            while (containerMatcher.find() && results.size() < maxResults) {
                String block = containerMatcher.group(1);

                String title = "";
                Matcher tm = titlePattern.matcher(block);
                if (tm.find()) {
                    title = tagPattern.matcher(tm.group(1)).replaceAll("").trim();
                }

                String snippet = "";
                Matcher sm = snippetPattern.matcher(block);
                if (sm.find()) {
                    String raw = sm.group(1) != null ? sm.group(1) : sm.group(2);
                    if (raw != null) {
                        snippet = decodeHtml(tagPattern.matcher(raw).replaceAll("").trim());
                    }
                }

                if (!snippet.isEmpty() && snippet.length() > 15) {
                    results.add(new SearchResult(decodeHtml(title), snippet));
                }
            }

            log.info("Baidu search found {} results", results.size());
        } catch (Exception e) {
            log.warn("Baidu search failed: {}", e.getMessage());
        }
        return results;
    }

    private String decodeHtml(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#x27;", "'")
                .trim();
    }

    public static class SearchResult {
        public String title;
        public String snippet;

        public SearchResult(String title, String snippet) {
            this.title = title;
            this.snippet = snippet;
        }

        public String toText() {
            return "【" + title + "】" + snippet;
        }
    }
}
