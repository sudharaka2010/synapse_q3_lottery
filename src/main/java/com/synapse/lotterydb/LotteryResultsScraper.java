package com.synapse.lotterydb;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LotteryResultsScraper {

    private static final String BASE_URL = "https://www.dlb.lk";
    private static final String RESULT_PAGE = BASE_URL + "/result/en";
    private static final String API_URL = BASE_URL + "/result/pagination_re";

    private static final DateTimeFormatter DLB_DATE =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd", Locale.ENGLISH);

    private static final Map<Integer, LotteryItem> LOTTERIES = new LinkedHashMap<>();
    static {
        LOTTERIES.put(1, new LotteryItem("Ada Kotipathi", 11, 16930));
        LOTTERIES.put(2, new LotteryItem("Lagna Wasana", 2, 16931));
        LOTTERIES.put(3, new LotteryItem("Super Ball", 3, 16932));
        LOTTERIES.put(4, new LotteryItem("Supiri Dhana Sampatha", 17, 16928));
        LOTTERIES.put(5, new LotteryItem("Kapruka", 12, 16934));
        LOTTERIES.put(6, new LotteryItem("Jayoda", 6, 16173));
        LOTTERIES.put(7, new LotteryItem("Jaya Sampatha", 18, 16929));
    }

    private static Map<String, String> sessionCookies = new HashMap<>();

    public static void printMenu() {
        for (Map.Entry<Integer, LotteryItem> e : LOTTERIES.entrySet()) {
            System.out.println(e.getKey() + ". " + e.getValue().name);
        }
    }

    public static ScrapedResult fetchByChoice(int choice) throws IOException {
        LotteryItem item = LOTTERIES.get(choice);
        if (item == null) return null;

        ensureSessionCookies();

        String html = fetchHtmlFromApi(item);

        // ✅ API returns a <tr> row (not full page), so parse that
        Result parsed = parseApiRowHtml(html, item.name);

        if (parsed.lotteryName == null || parsed.lotteryName.isBlank()) {
            throw new IOException("Lottery name not found in API response HTML.\n" +
                    "DEBUG (first 250 chars): " + preview(html));
        }
        if (parsed.drawNo == null || parsed.drawNo.isBlank()) {
            throw new IOException("Draw number not found in API response HTML.\n" +
                    "DEBUG (first 250 chars): " + preview(html));
        }
        if (parsed.dateIso == null || parsed.dateIso.isBlank()) {
            throw new IOException("Draw date not found in API response HTML.\n" +
                    "DEBUG (first 250 chars): " + preview(html));
        }

        ScrapedResult out = new ScrapedResult();
        out.lotteryName = parsed.lotteryName;
        out.drawNo = parsed.drawNo;
        out.date = parsed.dateIso;
        out.numbers = parsed.numbers == null ? "" : parsed.numbers.trim();
        out.title = parsed.lotteryName + " - Draw " + parsed.drawNo;
        out.url = RESULT_PAGE;

        return out;
    }

    private static void ensureSessionCookies() throws IOException {
        if (!sessionCookies.isEmpty()) return;

        Connection.Response res = Jsoup.connect(RESULT_PAGE)
                .method(Connection.Method.GET)
                .timeout(20_000)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .userAgent(browserUA())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .execute();

        // Some networks return 406; try base URL
        if (res.statusCode() == 406 || res.cookies().isEmpty()) {
            Connection.Response res2 = Jsoup.connect(BASE_URL + "/")
                    .method(Connection.Method.GET)
                    .timeout(20_000)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .userAgent(browserUA())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .execute();
            sessionCookies = res2.cookies();
        } else {
            sessionCookies = res.cookies();
        }

        if (sessionCookies.isEmpty()) {
            throw new IOException("Could not get session cookies from DLB. (No cookies received)");
        }
    }

    private static String fetchHtmlFromApi(LotteryItem item) throws IOException {
        Connection.Response res = Jsoup.connect(API_URL)
                .method(Connection.Method.POST)
                .timeout(20_000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .referrer(RESULT_PAGE)
                .userAgent(browserUA())
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Origin", BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .cookies(sessionCookies)
                .data("pageId", "0")
                .data("resultID", String.valueOf(item.resultId))
                .data("lotteryID", String.valueOf(item.lotteryId))
                .data("lastsegment", "en")
                .execute();

        if (res.statusCode() != 200) {
            throw new IOException("HTTP error fetching URL. Status=" + res.statusCode() + ", URL=[" + API_URL + "]");
        }

        return res.body();
    }

    /**
     * ✅ Parses the API response which is usually a <tr>...</tr> row
     * Example:
     * <tr><td>2891 | 2026-Jan-31 Saturday</td>
     *     <td><ul class="res_allnumber"><li class="res_eng_letter">D</li>...</ul></td>
     * </tr>
     */
    private static Result parseApiRowHtml(String html, String fallbackLotteryName) {
        Result r = new Result();
        r.lotteryName = fallbackLotteryName; // API row does not include the lottery name

        // Wrap in table so Jsoup can parse <tr> properly
        Document doc = Jsoup.parse("<table>" + html + "</table>");

        Element tr = doc.selectFirst("tr");
        if (tr == null) return r;

        Elements tds = tr.select("td");
        if (tds.isEmpty()) return r;

        // 1) First TD contains: "2891 | 2026-Jan-31 Saturday"
        String firstTdText = tds.get(0).text().trim();
        // Extract drawNo (left side of |)
        r.drawNo = extractDrawNoFromPipe(firstTdText);
        // Extract date (right side of |)
        r.dateIso = extractDateIsoFromPipe(firstTdText);

        // 2) Extract all li inside ul.res_allnumber (numbers + letters)
        List<String> tokens = new ArrayList<>();
        for (Element li : tr.select("ul.res_allnumber li")) {
            String t = li.text().trim();
            if (!t.isEmpty()) tokens.add(t);
        }

        // Build "numbers" output:
        // Keep letters like "D" as well (because some lotteries have letter+number code)
        r.numbers = String.join(" ", uniqueKeepOrder(tokens));

        return r;
    }

    private static String extractDrawNoFromPipe(String s) {
        // "2891 | 2026-Jan-31 Saturday"
        String[] parts = s.split("\\|");
        if (parts.length >= 1) {
            String left = parts[0].trim();
            Matcher m = Pattern.compile("\\b\\d{1,6}\\b").matcher(left);
            if (m.find()) return m.group();
        }
        return null;
    }

    private static String extractDateIsoFromPipe(String s) {
        // "2891 | 2026-Jan-31 Saturday"
        String[] parts = s.split("\\|");
        if (parts.length < 2) return null;

        String right = parts[1].trim(); // "2026-Jan-31 Saturday"
        String[] tokens = right.split("\\s+");
        if (tokens.length == 0) return null;

        String dlbDate = tokens[0].trim(); // "2026-Jan-31"
        try {
            LocalDate d = LocalDate.parse(dlbDate, DLB_DATE);
            return d.toString(); // yyyy-MM-dd
        } catch (Exception ignored) {
            return null;
        }
    }

    private static <T> List<T> uniqueKeepOrder(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    private static String preview(String s) {
        if (s == null) return "null";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() <= 250 ? s : s.substring(0, 250) + "...";
    }

    private static String browserUA() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
    }

    private static class LotteryItem {
        final String name;
        final int lotteryId;
        final int resultId;

        LotteryItem(String name, int lotteryId, int resultId) {
            this.name = name;
            this.lotteryId = lotteryId;
            this.resultId = resultId;
        }
    }

    private static class Result {
        String lotteryName;
        String drawNo;
        String dateIso;
        String numbers;
    }

    public static class ScrapedResult {
        public String lotteryName;
        public String url;
        public String drawNo;
        public String date;     // yyyy-MM-dd
        public String numbers;  // includes letters + numbers
        public String title;
    }
}
