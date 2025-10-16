package com.otd.otd_msa_back_life.exercise.repository;

import com.otd.otd_msa_back_life.body_composition.entity.BodyComposition;
import com.otd.otd_msa_back_life.exercise.entity.ExerciseRecord;
import com.otd.otd_msa_back_life.exercise.model.AverageExerciseDurationDto;
import com.otd.otd_msa_back_life.feign.model.ExerciseCountAndSum;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    ExerciseRecord findByUserIdAndExerciseRecordId(Long userId, Long exerciseRecordId);


    List<ExerciseRecord> findByUserIdAndStartAtBetween(
            Long userId,
            LocalDateTime startOfWeek,
            LocalDateTime endOfWeek
    );
    int countByUserIdAndStartAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(er) AS count, COALESCE(SUM(er.activityKcal), 0) AS totalKcal " +
            "FROM ExerciseRecord er " +
            "WHERE er.userId = :userId " +
            "AND er.startAt BETWEEN :start AND :end")
    ExerciseCountAndSum getDailyExerciseSummary(@Param("userId") Long userId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    //    @Query(value = """
//                    SELECT DATE(start_at) as startAt, AVG(duration) as averageDuration
//                    FROM exercise_record
//                    GROUP BY DATE(start_at)
//                    ORDER BY DATE(start_at)
//                   """
//            , nativeQuery = true)
//    List<AverageExerciseDurationDto> findAverageDurationGroupedByDateNative();

    void deleteByUserIdAndExerciseRecordId(Long userId, Long exerciseRecordId);


    @Query("""
            select e from ExerciseRecord e
                where e.userId = :userId
                    and FUNCTION('DATE', e.startAt) = :mealDay
            """)
    List<ExerciseRecord> findByUserIdAndStartDate(
            @Param("userId") Long userId,
            @Param("mealDay") LocalDate mealDay
    );

    void deleteAllByUserId(Long userId);

    List<ExerciseRecord> findAllByUserId(Long userId);

    // 대시보드
    // 총 운동 기록 수
    @Query("SELECT COUNT(e) FROM ExerciseRecord e")
    int countTotalRecord();

    // 이번 주 기록 유저 수
    @Query("""
        SELECT COUNT(DISTINCT e.userId)
        FROM ExerciseRecord e
        WHERE e.startAt >= :monday
    """)
    int countWeeklyRecordUser(@Param("monday") LocalDateTime monday);
}
