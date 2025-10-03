package com.otd.otd_msa_back_life.feign.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseDataReq {
    private Long userId;
    private Long recordId;
    private String name;
    private Double record;
    private LocalDate recordDate;
    private LocalDate today;
    private int count;
}
