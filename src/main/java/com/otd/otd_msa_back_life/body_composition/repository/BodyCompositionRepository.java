package com.otd.otd_msa_back_life.body_composition.repository;

import com.otd.otd_msa_back_life.body_composition.entity.BasicBodyInfo;
import com.otd.otd_msa_back_life.body_composition.entity.BodyComposition;
import com.otd.otd_msa_back_life.body_composition.model.MinMaxDateDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BodyCompositionRepository extends JpaRepository<BodyComposition, Long> {
    Optional<BodyComposition> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<BodyComposition> findTopByUserIdOrderByCreatedAtAsc(Long userId);
    List<BodyComposition> findByUserIdAndDeviceTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long userId
            , String deviceType
            , LocalDateTime startDate
            , LocalDateTime endDate
    );
    List<BodyComposition> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long userId
            , LocalDateTime startDate
            , LocalDateTime endDate
    );

    @Query("""
            select b from BodyComposition b
                where b.userId = :userId
                    and FUNCTION('DATE', b.createdAt) = :mealDay
            """)
    BodyComposition findByUserIdAndCreatedDate(
            @Param("userId") Long userId,
            @Param("mealDay") LocalDate mealDay
    );


    @Query("""
            SELECT new com.otd.otd_msa_back_life.body_composition.model.MinMaxDateDto(MIN(b.createdAt), MAX(b.createdAt))
            FROM BodyComposition b WHERE b.userId = :userId
            """)
    MinMaxDateDto findMinMaxDatesByUserId(@Param("userId") Long userId);


}
