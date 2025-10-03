package com.otd.otd_msa_back_life.meal.service;

import com.otd.otd_msa_back_life.feign.ChallengeFeignClient;
import com.otd.otd_msa_back_life.feign.model.ExerciseDataReq;
import com.otd.otd_msa_back_life.feign.model.MealDataReq;
import com.otd.otd_msa_back_life.meal.entity.*;
import com.otd.otd_msa_back_life.meal.model.InputMealRecordDetailDto;
import com.otd.otd_msa_back_life.meal.model.InputMealRecordReq;
import com.otd.otd_msa_back_life.meal.repository.MealFoodDbRepository;
import com.otd.otd_msa_back_life.meal.repository.MealFoodMakeDbRepository;
import com.otd.otd_msa_back_life.meal.repository.MealRecordDetailRepository;
import com.otd.otd_msa_back_life.meal.repository.MealRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Service
public class MealService {
    private final MealFoodDbRepository mealFoodDbRepository;
    private final MealFoodMakeDbRepository mealFoodMakeDbRepository;
    private final MealRecordRepository mealRecordRepository;
    private final MealRecordDetailRepository mealRecordDetailRepository;
    private final ChallengeFeignClient challengeFeignClient;

    public List<MealFoodDb> findFood(String foodName)
    {
        int limit = 20;// 검색할때 몇개 까지 받을 건지
        List<MealFoodDb> mealFoodDb = mealFoodDbRepository.findByFoodNameContaining(foodName,Limit.of(limit));
        return mealFoodDb;
    }

    public MealSaveResultDto inputMealData (Long userId, InputMealRecordReq mealRecordReq)
    {
        List<Long> recordIds = new ArrayList<>();
        List<Long> newUserFoodIds = new ArrayList<>();
        int totalKcal = 0;
        float totalProtein =0;
        float totalCarbohydrate =0;
        float totalFat =0;
        float totalSugar =0;
        float totalNatrium =0;




       int result = mealRecordDetailRepository.deleteByUserIdAndMealRecordIdsMealDayAndMealRecordIdsMealTime(userId,mealRecordReq.getMealDay(),mealRecordReq.getMealTime());
       int result2 = mealRecordRepository.deleteByUserIdAndMealRecordIdsMealDayAndMealRecordIdsMealTime(userId,mealRecordReq.getMealDay(),mealRecordReq.getMealTime());

        log.info("삭제 결롱 {}  ,   {} ", result, result2);
        MealRecordIds mealRecordIds =
                new MealRecordIds(mealRecordReq.getMealTime(), mealRecordReq.getMealDay());
        for (InputMealRecordDetailDto listFood : mealRecordReq.getFoods()) {
            totalKcal         += listFood.getKcal();
            totalProtein      += listFood.getProtein();
            totalCarbohydrate += listFood.getCarbohydrate();
            totalFat          += listFood.getFat();
            totalSugar        += listFood.getSugar();
            totalNatrium      += listFood.getNatrium();

            MealRecord mealRecord;

            if (listFood.getFoodDbId() == null) {
                // 사용자 음식 없으면  입력
                MealFoodMakeDb userFood = mealFoodMakeDbRepository
                        .findByUserIdAndFoodName(userId, listFood.getFoodName());

                if (userFood == null) {
                    userFood = MealFoodMakeDb.builder()
                            .userId(userId)
                            .foodName(listFood.getFoodName())
                            .flag(listFood.getFlag())
                            .kcal(listFood.getKcal())
                            .protein(listFood.getProtein())
                            .carbohydrate(listFood.getCarbohydrate())
                            .fat(listFood.getFat())
                            .sugar(listFood.getSugar())
                            .natrium(listFood.getNatrium())
                            .amount(listFood.getAmount())
                            .build();

                    userFood = mealFoodMakeDbRepository.save(userFood); // 저장 후 반환값 사용 권장
                    newUserFoodIds.add(userFood.getUserFoodId());
                }

                mealRecord = MealRecord.builder()
                        .userFood(userFood)
                        .foodDb(null)
                        .mealRecordIds(mealRecordIds)
                        .userId(userId)
                        .foodAmount(listFood.getAmount())
                        .build();
              
            } else {
                // 공용 음식
                MealFoodDb foodDb = mealFoodDbRepository.findByFoodDbId(listFood.getFoodDbId());
          

                mealRecord = MealRecord.builder()
                        .foodDb(foodDb)
                        .userFood(null)
                        .mealRecordIds(mealRecordIds)
                        .userId(userId)
                        .foodAmount(listFood.getAmount())
                        .build();
            }



            MealRecord saved = mealRecordRepository.save(mealRecord);
            recordIds.add(saved.getMealId());
        }

        MealRecordDetail mealRecordDetail= MealRecordDetail.builder()
                .userId(userId)
                .mealRecordIds(mealRecordIds)
                .totalKcal(totalKcal)
                .totalProtein(totalProtein)
                .totalCarbohydrate(totalCarbohydrate)
                .totalFat(totalFat)
                .totalSugar(totalSugar)
                .totalNatrium(totalNatrium)
                .build();
        mealRecordDetail = mealRecordDetailRepository.save(mealRecordDetail);

        Double sumProtein = mealRecordDetailRepository.findTotalProteinByUserAndDay(userId, mealRecordReq.getMealDay());

        if (sumProtein >= 100) {
            String challengeName = "단백질 섭취";
            MealDataReq feign = MealDataReq.builder()
                    .userId(userId)
                    .mealDay(mealRecordReq.getMealDay())
                    .today(LocalDate.now())
                    .totalProtein(sumProtein)
                    .name(challengeName)
                    .build();
            ResponseEntity<Integer> response = challengeFeignClient.updateProgressByMeal(feign);
            Integer feignResult = response.getBody();
        }


        return new MealSaveResultDto(recordIds.size(), recordIds, newUserFoodIds);
    }

//    public List<MealFoodDb> findFood(String foodName) {
//        int limit = 20;
//        String[] keywords = foodName.split("\\s+");
//
//        List<MealFoodDb> result = new ArrayList<>();
//        for (String keyword : keywords) {
//            result.addAll(mealFoodDbRepository.findByFoodNameContaining(keyword, Limit.of(limit)));
//        }
//        return result.stream()
//                .distinct()
//                .limit(limit)
//                .toList();
//    }

}
