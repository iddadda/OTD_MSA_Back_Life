package com.otd.otd_msa_back_life.feign;

import com.otd.otd_msa_back_life.feign.model.ExerciseDataReq;
import com.otd.otd_msa_back_life.feign.model.ChallengeRecordDeleteReq;
import com.otd.otd_msa_back_life.feign.model.MealDataReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "challenge",
        url = "${constants.feign-client.challenge.url}")
public interface ChallengeFeignClient {

    @PostMapping("/progress/exercise")
    ResponseEntity<Integer> updateProgressByExercise(@RequestBody ExerciseDataReq req);

    @DeleteMapping("/record/delete")
    ResponseEntity<Integer> deleteRecordByExercise(@RequestBody ChallengeRecordDeleteReq req);

    @PostMapping("/progress/meal")
    ResponseEntity<Integer> updateProgressByMeal(@RequestBody MealDataReq req);
}
