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
    public void run(String... args) throws Exception {

        System.out.println("=== Q3: Scrape + Save Lottery Results to MySQL (Hibernate/JPA) ===");
        System.out.println("1. Ada Kotipathi");
        System.out.println("2. Lagna Wasana");
        System.out.println("3. Super Ball");
        System.out.println("4. Govi Setha");
        System.out.println("5. Dhana Nidhanaya");
        System.out.println("6. Mahajana Sampatha");
        System.out.println("7. Jaya Sampatha");

        try (Scanner sc = new Scanner(System.in)) {

            System.out.print("\nSelect lottery (1-7): ");
            int choice = Integer.parseInt(sc.nextLine().trim());

            // 1) scrape
            LotteryResultsScraper.ScrapedResult r = LotteryResultsScraper.fetchByChoice(choice);

            if (r == null) {
                System.out.println("Invalid selection. Exiting.");
                return;
            }

            // 2) validate date (avoid crash)
            if (r.date == null || r.date.isBlank()) {
                System.out.println("Date not found from website. Not saving to DB.");
                return;
            }

            int drawNoInt = Integer.parseInt(r.drawNo);

            // 3) prevent duplicates
            if (repo.findByLotteryNameAndDrawNo(r.lotteryName, drawNoInt).isPresent()) {
                System.out.println("Already saved in DB (same lottery + draw).");
                return;
            }

            // 4) map to entity
            LotteryResult26 row = new LotteryResult26();
            row.setLotteryName(r.lotteryName);
            row.setDrawNo(drawNoInt);
            row.setDrawDate(Date.valueOf(r.date));       // yyyy-MM-dd
            row.setResultNumbers(r.numbers);
            row.setSourceUrl(r.url);

            // 5) save
            repo.save(row);

            System.out.println("\n Saved to DB table: lottery_result26");
            System.out.println("Lottery : " + r.lotteryName);
            System.out.println("Draw No : " + r.drawNo);
            System.out.println("Date    : " + r.date);
            System.out.println("Numbers : " + r.numbers);
        }
    }
}
