package com.synapse.lotterydb.repo;

import com.synapse.lotterydb.entity.LotteryResult26;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LotteryResult26Repo extends JpaRepository<LotteryResult26, Integer> {
    Optional<LotteryResult26> findByLotteryNameAndDrawNo(String lotteryName, Integer drawNo);
}
