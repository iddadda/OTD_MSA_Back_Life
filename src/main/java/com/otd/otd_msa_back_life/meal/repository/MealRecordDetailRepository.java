package com.otd.otd_msa_back_life.meal.repository;

import com.otd.otd_msa_back_life.meal.entity.MealFoodMakeDb;
import com.otd.otd_msa_back_life.meal.entity.MealRecordDetail;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface MealRecordDetailRepository extends JpaRepository<MealRecordDetail, Long> {
    @Modifying
    @Transactional
    int deleteByUserIdAndMealRecordIdsMealDayAndMealRecordIdsMealTime(
            Long userId, LocalDate mealDay, String mealTime
    );

    @Query("SELECT COALESCE(SUM(m.totalProtein), 0) " +
            "FROM MealRecordDetail m " +
            "WHERE m.userId = :userId " +
            "AND m.mealRecordIds.mealDay = :mealDay")
    Double findTotalProteinByUserAndDay(@Param("userId") Long userId,
                                        @Param("mealDay") LocalDate mealDay);
}