package com.otd.otd_msa_back_life.exercise.service;

import com.otd.otd_msa_back_life.common.model.PagingDto;
import com.otd.otd_msa_back_life.common.model.PagingReq;
import com.otd.otd_msa_back_life.exercise.entity.ExerciseCatalog;
import com.otd.otd_msa_back_life.exercise.entity.ExerciseRecord;
import com.otd.otd_msa_back_life.exercise.mapper.ExerciseRecordMapper;
import com.otd.otd_msa_back_life.exercise.model.ExerciseRecordDetailGetRes;
import com.otd.otd_msa_back_life.exercise.model.ExerciseRecordGetRes;
import com.otd.otd_msa_back_life.exercise.model.ExerciseRecordPostReq;
import com.otd.otd_msa_back_life.exercise.model.ExerciseRecordWeeklyGetReq;
import com.otd.otd_msa_back_life.exercise.repository.ExerciseCatalogRepository;
import com.otd.otd_msa_back_life.exercise.repository.ExerciseRecordRepository;
import com.otd.otd_msa_back_life.feign.ChallengeFeignClient;
import com.otd.otd_msa_back_life.feign.model.ExerciseDataReq;
import com.otd.otd_msa_back_life.feign.model.ChallengeRecordDeleteReq;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseRecordService {
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final ExerciseCatalogRepository exerciseCatalogRepository;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final ChallengeFeignClient challengeFeignClient;

    //    [post] exerciseRecord
    @Transactional
    public Long saveExerciseRecord(Long userId, ExerciseRecordPostReq req) {
//        exercise 존재 여부 확인
        ExerciseCatalog exercise = exerciseCatalogRepository.findById(req.getExerciseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 운동입니다."));

//        distance 입력 유효성 검사
        if(exercise.getHasDistance() && req.getDistance() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거리기반 운동은 distance 입력이 필수입니다.");
        }
        if (!exercise.getHasDistance() && req.getDistance() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거리 기반 운동이 아닌 경우 distance 입력이 불가합니다.");
        }
        
//        service 입력 유효성 검사
        if (exercise.getHasReps() && req.getReps() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반복횟수기반 운동은 reps 입력이 필수입니다. ");
        }
        if (!exercise.getHasReps() && req.getReps() != null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반복횟수기반 운동은 reps 입력이 불가합니다. ");
        }

//                exerciseRecord 생성
        ExerciseRecord exerciseRecord = ExerciseRecord.builder()
                .activityKcal(req.getActivityKcal())
                .effortLevel(req.getEffortLevel())
                .exercise(exercise)
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .distance(req.getDistance())
                .reps(req.getReps())
                .userId(userId)
                .build();
        Long recordId = exerciseRecordRepository.save(exerciseRecord).getExerciseRecordId();
        if (req.getReps() != null || req.getDistance() != null) {
            int count = countExerciseRecordByDate(userId, req.getStartAt().toLocalDate());
            ExerciseDataReq feign = ExerciseDataReq.builder()
                    .userId(userId)
                    .recordId(recordId)
                    .name(exercise.getExerciseName())
                    .record(exercise.getHasDistance() ? req.getDistance() : req.getReps().doubleValue())
                    .recordDate(req.getStartAt().toLocalDate())
                    .count(count)
                    .today(LocalDate.now())
                    .build();
            ResponseEntity<Integer> response = challengeFeignClient.updateProgressByExercise(feign);
            Integer feignResult = response.getBody();
        }

        return recordId;
    }

    //    [GET] recordList -> page
    @Transactional
    public List<ExerciseRecordGetRes> getExerciseRecordList(Long userId, PagingReq req) {
        PagingDto dto = PagingDto.builder()
                .type(req.getType())
                .date(req.getDate())
                .size(req.getRowPerPage())
                .startIdx((req.getPage() - 1) * req.getRowPerPage())
                .userId(userId)
                .build();
        return exerciseRecordMapper.findByLimitTo(dto);
    }

    //    [GET] weeklyRecord
    @Transactional
    public List<ExerciseRecord> getExerciseRecordWeekly(Long userId, ExerciseRecordWeeklyGetReq req) {
        return exerciseRecordRepository.findByUserIdAndStartAtBetween(userId, req.getStartOfWeek(), req.getEndOfWeek());
    }

//    [GET] detail
    @Transactional
    public ExerciseRecordDetailGetRes getExerciseRecordDetail(Long userId, Long exerciseRecordId) {

        ExerciseRecord exerciseRecord = exerciseRecordRepository
                                        .findByUserIdAndExerciseRecordId(userId, exerciseRecordId);
        ExerciseCatalog exercise = exerciseCatalogRepository.findById(exerciseRecord.getExercise().getExerciseId())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않은 운동입니다."));

        ExerciseRecordDetailGetRes result = ExerciseRecordDetailGetRes.builder()
                .exerciseRecordId(exerciseRecordId)
                .distance(exerciseRecord.getDistance())
                .reps(exerciseRecord.getReps())
                .activityKcal(exerciseRecord.getActivityKcal())
                .effortLevel(exerciseRecord.getEffortLevel())
                .startAt(exerciseRecord.getStartAt())
                .endAt(exerciseRecord.getEndAt())
                .exerciseId(exercise.getExerciseId())
                .exerciseName(exercise.getExerciseName())
                .build();

        return result;
    }

//    [DELETE]
    @Transactional
    public void deleteExerciseRecord(Long userId, Long exerciseRecordId) {
        ExerciseRecord record = exerciseRecordRepository.findById(exerciseRecordId)
                .orElseThrow(() -> new IllegalArgumentException("운동 기록을 찾을 수 없습니다."));

        if(!record.getUserId().equals(userId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운동 기록을 삭제할 수 없습니다.");
        }
        ChallengeRecordDeleteReq req = ChallengeRecordDeleteReq.builder()
                .userId(userId)
                .name(record.getExercise().getExerciseName())
                .recordId(record.getExerciseRecordId())
                .recordDate(record.getStartAt().toLocalDate())
                .today(LocalDate.now())
                .build();
        exerciseRecordRepository.delete(record);
        int count = countExerciseRecordByDate(userId, record.getStartAt().toLocalDate());
        req.setCount(count);
        ResponseEntity<Integer> response = challengeFeignClient.deleteRecordByExercise(req);
        Integer feignResult = response.getBody();
//        exerciseRecordRepository.deleteByUserIdAndExerciseRecordId(userId, exerciseRecordId);
    }

    // challenge에 delete때 보내는 삭제하는 날의 운동 기록들
    private int countExerciseRecordByDate(Long userId, LocalDate recordDate) {
        return exerciseRecordRepository.countByUserIdAndStartAtBetween(
                userId,
                recordDate.atStartOfDay(),
                recordDate.plusDays(1).atStartOfDay()
        );
    }
}
