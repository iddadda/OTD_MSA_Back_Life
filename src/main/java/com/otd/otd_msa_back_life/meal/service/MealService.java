package com.otd.otd_msa_back_life.meal.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.otd.otd_msa_back_life.admin.mapper.AdminMapper;
import com.otd.otd_msa_back_life.admin.model.AdminMealDataDto;
import com.otd.otd_msa_back_life.body_composition.entity.BodyComposition;
import com.otd.otd_msa_back_life.body_composition.repository.BodyCompositionRepository;
import com.otd.otd_msa_back_life.exercise.entity.ExerciseRecord;
import com.otd.otd_msa_back_life.exercise.repository.ExerciseRecordRepository;
import com.otd.otd_msa_back_life.feign.ChallengeFeignClient;
import com.otd.otd_msa_back_life.feign.model.MealDataReq;
import com.otd.otd_msa_back_life.meal.entity.*;
import com.otd.otd_msa_back_life.meal.model.*;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


@Slf4j
@RequiredArgsConstructor
@Service
public class MealService {
    private final MealFoodDbRepository mealFoodDbRepository;
    private final MealFoodMakeDbRepository mealFoodMakeDbRepository;
    private final MealRecordRepository mealRecordRepository;
    private final MealRecordDetailRepository mealRecordDetailRepository;
    private final ChallengeFeignClient challengeFeignClient;

    // 운동 기록 가져올려는거
    private final BodyCompositionRepository bodyCompositionRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final AdminMapper adminMapper;

    public List<FoodSearchResultDto> findFood(String foodName , Long userId)
    {
        int limit = 20;// 검색할때 몇개 까지 받을 건지
//        List<FoodSearchResultDto> mealFoodDb = mealFoodDbRepository.findByFoodNameContaining(foodName,Limit.of(limit));
//        return mealFoodDb;
        // 시스템 음식 -> makeFoodId 는 null 로
        List<FoodSearchResultDto> mealFoodDbs = mealFoodDbRepository
                .findByFoodNameContaining(foodName, Limit.of(limit)).stream()
                .map(m -> FoodSearchResultDto.builder()
                        .foodDbId(m.getFoodDbId())
                        .foodName(m.getFoodName())
                        .flag(m.getFlag())
                        .kcal(m.getKcal())
                        .protein(m.getProtein())
                        .carbohydrate(m.getCarbohydrate())
                        .fat(m.getFat())
                        .sugar(m.getSugar())
                        .natrium(m.getNatrium())
                        .foodCapacity(m.getFoodCapacity())
                        .build()
                )
                .toList();

        // 사용자 제작 음식 -> systemFoodId 는 null 로, makeFoodId 에 userFoodId 세팅
        List<FoodSearchResultDto> userFoods = mealFoodMakeDbRepository
                .findByUserIdAndFoodNameContaining(userId, foodName,  Limit.of(limit)).stream()
                .map(u -> FoodSearchResultDto.builder()
                        .foodDbId(null)
                        .foodName(u.getFoodName())
                        .flag(u.getFlag())
                        .kcal(u.getKcal())
                        .protein(u.getProtein())
                        .carbohydrate(u.getCarbohydrate())
                        .fat(u.getFat())
                        .sugar(u.getSugar())
                        .natrium(u.getNatrium())
                        .foodCapacity(1)
                        .build()
                )
                .toList();

        // 합치고 필요하면 정렬/중복제거/최종 리밋
        return Stream.concat( userFoods.stream(), mealFoodDbs.stream())
                .limit(limit*2)
                .toList();
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

        MealRecordIds mealRecordIds =
                new MealRecordIds(mealRecordReq.getMealTime(), mealRecordReq.getMealDay());


       int result = mealRecordDetailRepository.deleteByUserIdAndMealRecordIdsMealDayAndMealRecordIdsMealTime(userId,mealRecordReq.getMealDay(),mealRecordReq.getMealTime());
       int result2 = mealRecordRepository.deleteByUserIdAndMealRecordIdsMealDayAndMealRecordIdsMealTime(userId,mealRecordReq.getMealDay(),mealRecordReq.getMealTime());

        log.info("삭제 결롱 {}  ,   {} ", result, result2);
        if (mealRecordReq.getFoods().isEmpty())
        {
            return new MealSaveResultDto(-1, null, null);
        }
// 금식 단식 했어요 부분
        if (!mealRecordReq.getFoods().isEmpty()) {
            InputMealRecordDetailDto food = mealRecordReq.getFoods().get(0);
            if (food.getFoodDbId() != null && food.getFoodDbId() == -10000L) {
                MealRecordIds ids = new MealRecordIds(mealRecordReq.getMealTime(), mealRecordReq.getMealDay());
                MealFoodDb dummy =  mealFoodDbRepository.findByFoodDbId(food.getFoodDbId()); // fetch 안 함

                MealRecord saved = mealRecordRepository.save(
                        MealRecord.builder()
                                .userId(userId)
                                .mealRecordIds(ids)
                                .foodAmount(1)
                                .foodDb(dummy)   //  더미 food 참조
                                .userFood(null)
                                .build()
                );

                return new MealSaveResultDto(1, List.of(saved.getMealId()),newUserFoodIds );
            }
            else if (food.getFoodDbId() != null && food.getFoodDbId() == 0) {
                return new MealSaveResultDto(-1, null, null);
            }
        }


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

            Double sumProteinObj = mealRecordDetailRepository.findTotalProteinByUserAndDay(userId, mealRecordReq.getMealDay());
            double sumProtein = sumProteinObj == null ? 0.0 : sumProteinObj;
            log.info("[meal] user={}, day={}, sumProtein={}", userId, mealRecordReq.getMealDay(), sumProtein);

            if (sumProtein >= 100) {
                String challengeName = "단백질 섭취";
                try {
                    ResponseEntity<List<String>> myChallenges =
                            challengeFeignClient.getActiveChallengeNames(userId, mealRecordReq.getMealDay());
                    List<String> activeChallenges = myChallenges.getBody();

                    if (activeChallenges != null && !activeChallenges.isEmpty()) {
                        for (String activeChallenge : activeChallenges) {
                            if (activeChallenge.equals(challengeName)) {
                                MealDataReq feign = MealDataReq.builder()
                                        .userId(userId)
                                        .recDate(mealRecordReq.getMealDay())
                                        .today(LocalDate.now())
                                        .value(sumProtein)
                                        .name(challengeName)
                                        .build();

                                try {
                                    ResponseEntity<Integer> response =
                                            challengeFeignClient.updateProgressByMeal(feign);
                                    Integer feignResult = response.getBody();

                                    if (response.getStatusCode().is2xxSuccessful()) {
                                        log.info("단백질 섭취 챌린지 진행 상황 업데이트 성공: {}", feignResult);
                                    } else {
                                        log.warn("단백질 섭취 챌린지 업데이트 실패: status={}", response.getStatusCode());
                                    }
                                } catch (Exception e) {
                                    log.error("단백질 섭취 챌린지 진행 업데이트 중 예외 발생: {}", e.getMessage(), e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("단백질 섭취 챌린지 활성화 여부 조회 중 예외 발생: {}", e.getMessage(), e);
                }
            }
        }
        return new MealSaveResultDto( result!=0 || result2!=0  ? -1 : recordIds.size()  , recordIds, newUserFoodIds);
    }

    public List<MealRecord> mealMainListRes(Long userId , LocalDate mealDay) {

            List<MealRecord> mealRecord = mealRecordRepository.findForMain(userId,mealDay);
        log.info("<UNK> <UNK> <UNK> {} ", mealRecord);

        return mealRecord;
    }


    public GetMyDayDateDto getToDay (Long userId, LocalDate mealDay) {
        BodyComposition bodyComposition = bodyCompositionRepository.findByUserIdAndCreatedDate(userId,mealDay);
        List<ExerciseRecord> exerciseRecord = exerciseRecordRepository.findByUserIdAndCreatedDate(userId,mealDay);

        int totalKcal =0;
        for (ExerciseRecord record : exerciseRecord) {
            totalKcal += record.getActivityKcal();
        }

        double basalMetabolicRate = (bodyComposition == null ? 0 : bodyComposition.getBasalMetabolicRate())  ;

        GetMyDayDateDto result = GetMyDayDateDto.builder()
                .selectDay(mealDay)
                .activityKcal(totalKcal)
                .basalMetabolicRate(basalMetabolicRate)
                .build();
        log.info("바디 :{} ", bodyComposition);
        log.info("운동 :{} ", exerciseRecord);

        return  result;
    }

    /** 일간 합계 */
    public GetSummaryTotalDto getDailyTotal(Long userId, LocalDate day) {
        // 재사용을 위한 다음날을 보냄
        LocalDate start = day;
        LocalDate end = day.plusDays(1);
        return mealRecordDetailRepository.sumBetween(userId, start, end);
    }

    /** 주간 합계 (요청에 start/end가 들어오는 케이스) */
    public GetSummaryTotalDto getWeeklyTotal(Long userId, LocalDate startDay, LocalDate endDay) {
        return mealRecordDetailRepository.sumBetween(userId, startDay, endDay);
    }

    /** 월간 합계 */
    public GetSummaryTotalDto getMonthlyTotal(Long userId, YearMonth ym) {
        LocalDate startDay = ym.atDay(1);
        LocalDate endDay = ym.plusMonths(1).atDay(1); // exclusive
        return mealRecordDetailRepository.sumBetween(userId, startDay, endDay);
    }

}
