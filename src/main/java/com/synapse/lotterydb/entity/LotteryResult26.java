package com.synapse.lotterydb.entity;

import jakarta.persistence.*;
import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lottery_result26",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lottery_draw",
                columnNames = {"lottery_name", "draw_no"}
        )
)
public class LotteryResult26 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "lottery_name", nullable = false, length = 50)
    private String lotteryName;

    @Column(name = "draw_no", nullable = false)
    private Integer drawNo;

    @Column(name = "draw_date", nullable = false)
    private Date drawDate;

    @Column(name = "result_numbers", nullable = false, length = 50)
    private String resultNumbers;

    @Column(name = "source_url", length = 255)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }

    public String getLotteryName() { return lotteryName; }
    public void setLotteryName(String lotteryName) { this.lotteryName = lotteryName; }

    public Integer getDrawNo() { return drawNo; }
    public void setDrawNo(Integer drawNo) { this.drawNo = drawNo; }

    public Date getDrawDate() { return drawDate; }
    public void setDrawDate(Date drawDate) { this.drawDate = drawDate; }

    public String getResultNumbers() { return resultNumbers; }
    public void setResultNumbers(String resultNumbers) { this.resultNumbers = resultNumbers; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
