package com.otd.otd_msa_back_life.feign;

import com.otd.otd_msa_back_life.feign.model.ExerciseDataReq;
import com.otd.otd_msa_back_life.feign.model.ChallengeRecordDeleteReq;
import com.otd.otd_msa_back_life.feign.model.MealDataReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "${constants.feign-client.challenge.name}" ,url = "${constants.feign-client.challenge.url}")
public interface ChallengeFeignClient {

    @PostMapping("/api/OTD/challenge/progress/exercise")
    ResponseEntity<Integer> updateProgressByExercise(@RequestBody ExerciseDataReq req);

    @DeleteMapping("/api/OTD/challenge/record/delete")
    ResponseEntity<Integer> deleteRecordByExercise(@RequestBody ChallengeRecordDeleteReq req);

    @PostMapping("/api/OTD/challenge/progress/meal")
    ResponseEntity<Integer> updateProgressByMeal(@RequestBody MealDataReq req);

    @GetMapping("/api/OTD/challenge/progress/challenges/{userId}")
    ResponseEntity<List<String>> getActiveChallengeNames(
            @PathVariable("userId") Long userId,
            @RequestParam("recordDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate
    );
}
