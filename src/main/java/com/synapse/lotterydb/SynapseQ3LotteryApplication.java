package com.synapse.lotterydb;

import com.synapse.lotterydb.entity.LotteryResult26;
import com.synapse.lotterydb.repo.LotteryResult26Repo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Date;
import java.util.Scanner;

@SpringBootApplication
public class SynapseQ3LotteryApplication implements CommandLineRunner {

    private final LotteryResult26Repo repo;

    public SynapseQ3LotteryApplication(LotteryResult26Repo repo) {
        this.repo = repo;
    }

    public static void main(String[] args) {
        SpringApplication.run(SynapseQ3LotteryApplication.class, args);
    }

    @Override
    public void run(String... args) {

        System.out.println("=== Q3: Scrape + Save Lottery Results (DLB) (Hibernate/JPA) ===");
        LotteryResultsScraper.printMenu();

        try (Scanner sc = new Scanner(System.in)) {

            System.out.print("\nSelect lottery (1-7): ");
            String input = sc.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number 1-7.");
                return;
            }

            // 1) scrape
            LotteryResultsScraper.ScrapedResult r;
            try {
                r = LotteryResultsScraper.fetchByChoice(choice);
            } catch (java.io.IOException e) {
                System.out.println("Failed to load results from DLB site.");
                System.out.println("Reason: " + e.getMessage());
                return;
            }

            if (r == null) {
                System.out.println("Invalid selection. Exiting.");
                return;
            }

            // 2) validate fields
            if (r.lotteryName == null || r.lotteryName.isBlank()) {
                System.out.println("Lottery name not found. Not saving to DB.");
                return;
            }
            if (r.drawNo == null || r.drawNo.isBlank()) {
                System.out.println("Draw number not found. Not saving to DB.");
                return;
            }
            if (r.date == null || r.date.isBlank()) {
                System.out.println("Draw date not found. Not saving to DB.");
                return;
            }

            int drawNoInt;
            try {
                drawNoInt = Integer.parseInt(r.drawNo.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid draw number from website. Not saving to DB.");
                return;
            }

            // ✅ Your requested logic:
            // If same lottery + same draw_no already exists -> "Today have not draw this lottery" (do not save)
            if (repo.findByLotteryNameAndDrawNo(r.lotteryName, drawNoInt).isPresent()) {
                System.out.println("\nToday have not draw this lottery.");
                System.out.println("Latest draw is already stored in DB.");
                System.out.println("Lottery : " + r.lotteryName);
                System.out.println("Draw No : " + r.drawNo);
                System.out.println("Date    : " + r.date);
                System.out.println("Numbers : " + r.numbers);
                return;
            }

            // ✅ New draw -> insert new row (keep old history)
            LotteryResult26 row = new LotteryResult26();
            row.setLotteryName(r.lotteryName);
            row.setDrawNo(drawNoInt);
            row.setDrawDate(Date.valueOf(r.date)); // yyyy-MM-dd
            row.setResultNumbers((r.numbers == null ? "" : r.numbers));
            row.setSourceUrl(r.url);

            repo.save(row);

            System.out.println("\n✅ New draw saved to DB table: lottery_result26");
            System.out.println("Lottery : " + r.lotteryName);
            System.out.println("Draw No : " + r.drawNo);
            System.out.println("Date    : " + r.date);
            System.out.println("Numbers : " + r.numbers);

        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}
