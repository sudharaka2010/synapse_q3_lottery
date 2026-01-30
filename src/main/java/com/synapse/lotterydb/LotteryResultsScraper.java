package com.synapse.lotterydb;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LotteryResultsScraper {

    // Base URL (given)
    private static final String BASE_URL = "https://results.govdoc.lk/";

    // Step 1+2: Lottery menu -> URL (given)
    private static final Map<Integer, LotteryItem> LOTTERIES = new LinkedHashMap<>();
    static {
        LOTTERIES.put(1, new LotteryItem("Ada Kotipathi", "https://results.govdoc.lk/results/ada-kotipathi-2888"));
        LOTTERIES.put(2, new LotteryItem("Lagna Wasana", "https://results.govdoc.lk/results/lagna-wasana-4774"));
        LOTTERIES.put(3, new LotteryItem("Super Ball", "https://results.govdoc.lk/results/super-ball-3062"));
        LOTTERIES.put(4, new LotteryItem("Govi Setha", "https://results.govdoc.lk/results/govi-setha-4330"));
        LOTTERIES.put(5, new LotteryItem("Dhana Nidhanaya", "https://results.govdoc.lk/results/dhana-nidhanaya-2118"));
        LOTTERIES.put(6, new LotteryItem("Mahajana Sampatha", "https://results.govdoc.lk/results/mahajana-sampatha-6088"));
        LOTTERIES.put(7, new LotteryItem("Jaya Sampatha", "https://results.govdoc.lk/results/jaya-sampatha-270"));
    }

    public static void main(String[] args) {
        System.out.println("=== Sri Lanka Lottery Results (GovDoc) ===");
        System.out.println("Base URL: " + BASE_URL);

        try (Scanner sc = new Scanner(System.in)) {

            printMenu();
            System.out.print("\nSelect lottery (1-" + LOTTERIES.size() + "): ");
            int choice = readInt(sc);

            LotteryItem item = LOTTERIES.get(choice);
            if (item == null) {
                System.out.println("Invalid selection. Exiting.");
                return;
            }

            System.out.println("\nFetching: " + item.name);
            System.out.println("URL     : " + item.url);

            Result result = fetchAndParse(item);

            // Step 4: Display using System.out
            System.out.println("\n==============================");
            System.out.println("Lottery : " + result.lotteryName);
            System.out.println("Title   : " + result.pageTitle);
            System.out.println("Draw No : " + (result.drawNo == null ? "N/A" : result.drawNo));
            System.out.println("Date    : " + (result.date == null ? "N/A" : result.date));

            if (!result.winningCodes.isEmpty()) {
                System.out.println("Codes   : " + String.join(", ", result.winningCodes));
            } else {
                System.out.println("Codes   : N/A");
            }

            if (!result.winningNumbers.isEmpty()) {
                System.out.println("Numbers : " + String.join(" ", result.winningNumbers));
            } else {
                System.out.println("Numbers : N/A (layout may differ — see notes below)");
            }

            System.out.println("==============================\n");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Tip: If the page structure changes, adjust selectors OR rely on regex extraction (already included).");
        }
    }

    // -------------------- Step 3: Fetch + Parse --------------------

    private static Result fetchAndParse(LotteryItem item) throws IOException {
        Document doc = Jsoup.connect(item.url)
                .userAgent("Mozilla/5.0")
                .timeout(15_000)
                .get();

        Result r = new Result();
        r.lotteryName = item.name;
        r.pageTitle = safeText(doc.selectFirst("h1"));
        r.drawNo = extractDrawNo(item.url, r.pageTitle);

        // Try to extract date from page text (supports 2026-01-29 / 29-01-2026 / 29/01/2026 style)
        String fullText = doc.text();
        r.date = extractDate(fullText);

        // Heuristic 1: Find “code-like” prizes (e.g., A 123456 / AB 123456)
        r.winningCodes.addAll(extractWinningCodes(fullText));

        // Heuristic 2: Extract winning numbers from common elements first (ul/li, badges, balls etc.)
        r.winningNumbers.addAll(extractNumbersFromHtml(doc));

        // If still empty, fallback: parse numbers from the full text
        if (r.winningNumbers.isEmpty()) {
            r.winningNumbers.addAll(extractNumbersFromText(fullText));
        }

        // Remove duplicates while keeping order
        r.winningCodes = uniqueKeepOrder(r.winningCodes);
        r.winningNumbers = uniqueKeepOrder(r.winningNumbers);

        return r;
    }

    public static ScrapedResult fetchByChoice(int choice) throws IOException {
        LotteryItem item = LOTTERIES.get(choice);
        if (item == null) return null;

        Result result = fetchAndParse(item);

        ScrapedResult out = new ScrapedResult();
        out.lotteryName = result.lotteryName;
        out.url = item.url;
        out.drawNo = result.drawNo;
        out.date = result.date;
        out.numbers = String.join(" ", result.winningNumbers);
        out.title = result.pageTitle;

        return out;
    }


    private static List<String> extractNumbersFromHtml(Document doc) {
        List<String> nums = new ArrayList<>();

        // Try common containers (works across many result page templates)
        Elements candidates = doc.select(
                "ul li, ol li, " +
                        ".ball, .balls span, .number, .numbers span, " +
                        ".winning, .winning-numbers span, " +
                        ".result, .results span, " +
                        "table td"
        );

        for (Element el : candidates) {
            String t = el.text().trim();
            // Accept 1-2 digit numbers (00-99) or 1-3 digits depending on lottery formats
            if (t.matches("\\d{1,3}")) {
                nums.add(t);
            }
        }
        return nums;
    }

    private static List<String> extractNumbersFromText(String text) {
        List<String> nums = new ArrayList<>();
        // Capture 1-2 digit tokens (typical lotto balls). You can change to \\d{1,3} if needed.
        Matcher m = Pattern.compile("\\b\\d{1,2}\\b").matcher(text);
        while (m.find()) {
            nums.add(m.group());
        }
        return nums;
    }

    private static List<String> extractWinningCodes(String text) {
        List<String> codes = new ArrayList<>();
        // Pattern: A123456, A 123456, AB123456, AB 123456
        Matcher m = Pattern.compile("\\b[A-Z]{1,2}\\s?\\d{6}\\b").matcher(text);
        while (m.find()) {
            codes.add(m.group().replaceAll("\\s+", ""));
        }
        return codes;
    }

    private static String extractDate(String text) {
        // Try ISO: 2026-01-29
        Matcher iso = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b").matcher(text);
        if (iso.find()) return iso.group();

        // Try: 29/01/2026
        Matcher slash = Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b").matcher(text);
        if (slash.find()) return slash.group();

        // Try: 29-01-2026
        Matcher dash = Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}\\b").matcher(text);
        if (dash.find()) return dash.group();

        return null;
    }

    private static String extractDrawNo(String url, String title) {
        // Prefer number at end of URL (your links have it)
        Matcher m = Pattern.compile("-(\\d+)\\b").matcher(url);
        String last = null;
        while (m.find()) last = m.group(1);
        if (last != null) return last;

        // Fallback: any big number in title
        if (title != null) {
            Matcher t = Pattern.compile("\\b\\d{3,6}\\b").matcher(title);
            if (t.find()) return t.group();
        }
        return null;
    }

    // -------------------- Helpers --------------------

    private static void printMenu() {
        System.out.println("\nAvailable lotteries:");
        for (Map.Entry<Integer, LotteryItem> e : LOTTERIES.entrySet()) {
            System.out.println(e.getKey() + ". " + e.getValue().name);
        }
    }

    private static int readInt(Scanner sc) {
        while (true) {
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                System.out.print("Enter a valid number: ");
            }
        }
    }

    private static String safeText(Element el) {
        return el == null ? "N/A" : el.text().trim();
    }

    private static <T> List<T> uniqueKeepOrder(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    // -------------------- Models --------------------

    private static class LotteryItem {
        final String name;
        final String url;
        LotteryItem(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private static class Result {
        String lotteryName;
        String pageTitle;
        String drawNo;
        String date;
        List<String> winningCodes = new ArrayList<>();
        List<String> winningNumbers = new ArrayList<>();
    }

    public static class ScrapedResult {
        public String lotteryName;
        public String url;
        public String drawNo;     // "2888"
        public String date;       // "2026-01-28"
        public String numbers;    // "05 41 56 64"
        public String title;
    }

}
