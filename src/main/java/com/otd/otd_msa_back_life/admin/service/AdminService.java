package com.otd.otd_msa_back_life.admin.service;

import com.otd.otd_msa_back_life.admin.mapper.AdminMapper;
import com.otd.otd_msa_back_life.admin.model.*;
import com.otd.otd_msa_back_life.admin.model.AdminCommunityDataDto;
import com.otd.otd_msa_back_life.admin.model.AdminCommunityGetRes;
import com.otd.otd_msa_back_life.admin.model.AdminExerciseDto;
import com.otd.otd_msa_back_life.admin.model.dashboard.*;
import com.otd.otd_msa_back_life.admin.model.statistics.*;
import com.otd.otd_msa_back_life.community.entity.CommunityCategory;
import com.otd.otd_msa_back_life.community.entity.CommunityPost;
import com.otd.otd_msa_back_life.community.repository.*;
import com.otd.otd_msa_back_life.configuration.model.ResultResponse;
import com.otd.otd_msa_back_life.exercise.repository.ExerciseRecordRepository;
import com.otd.otd_msa_back_life.meal.entity.MealFoodDb;
import com.otd.otd_msa_back_life.meal.entity.MealFoodMakeDb;
import com.otd.otd_msa_back_life.meal.entity.MealRecordDetail;
import com.otd.otd_msa_back_life.feign.ChallengeFeignClient;
import com.otd.otd_msa_back_life.meal.repository.MealFoodDbRepository;
import com.otd.otd_msa_back_life.meal.repository.MealFoodMakeDbRepository;
import com.otd.otd_msa_back_life.meal.repository.MealRecordDetailRepository;
import com.otd.otd_msa_back_life.water_intake.repository.DailyWaterIntakeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminMapper adminMapper;

    private final MealRecordDetailRepository mealRecordDetailRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final DailyWaterIntakeRepository dailyWaterIntakeRepository;

    private final MentRepository mentRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityLikeRepository communityLikeRepository;
    private final CommunityPostFileRepository communityPostFileRepository;
    private final ChallengeFeignClient challengeFeignClient;
    private final CommunityCategoryRepository communityCategoryRepository;
    private final MealFoodMakeDbRepository mealFoodMakeDbRepository;
    private final MealFoodDbRepository mealFoodDbRepository;

    public List<AdminCommunityGetRes> getCommunity() {
        List<AdminCommunityGetRes> posts = adminMapper.findAllCommunity();
        return posts;
    }

    public List<AdminExerciseDto> getExerciseData(Long userId){
        return adminMapper.findExerciseDataByUserId(userId);
    }

    public List<MealRecordDetail> getMealData(Long userId) {
        return mealRecordDetailRepository.findAllByUserId(userId);
    }

    public List<AdminMealDetailGetRes> getMealDetail(AdminMealDetailGetReq req) {
        return adminMapper.findMealDetailByUserIdAndMealDayAndMealTime(req);
    }

    // 대시보드 커뮤니티
    public AdminDashBoardCommunityDto getCommunityDashBoard(){
        AdminDashBoardCommunityDto dto = new AdminDashBoardCommunityDto();

        // 총 게시물 수
        int totalPostCount = communityPostRepository.countAllPost();
        // 이번주 게시글 수
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime startOfWeek = monday.atStartOfDay();
        int totalWeeklyPostCount = communityPostRepository.countWeeklyPost(startOfWeek);
        // 카테고리별 게시글 수
        List<CategoryPostCountRes> categoryPostCountRes = communityPostRepository.countPostByCategory();
        // 좋아요 top5 게시글
        List<TopLikePostRes> topLikePostRes = adminMapper.getTop5PostByLike();
        // 댓글 top5 게시글
        List<TopCommentPostRes> topCommentPostRes = adminMapper.getTop5PostByComment();

        dto.setTotalPostCount(totalPostCount);
        dto.setWeeklyNewPostCount(totalWeeklyPostCount);
        dto.setCategoryPostCount(categoryPostCountRes);
        dto.setTopLikePost(topLikePostRes);
        dto.setTopCommentPost(topCommentPostRes);

        return dto;
    }
    public AdminMealDto searchMeals(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("foodName").ascending());

        Page<MealFoodDb> dbPage;
        if (keyword != null && !keyword.isBlank()) {
            dbPage = mealFoodDbRepository.findByFoodNameContaining(keyword, pageable);
        } else {
            dbPage = mealFoodDbRepository.findAll(pageable);
        }

        // 커스텀 음식은 전체 조회 (필요하다면 여기도 검색/페이징 추가 가능)
        List<MealFoodMakeDb> makeFoods = mealFoodMakeDbRepository.findAll();

        AdminMealDto dto = new AdminMealDto();
        dto.setMealFoodDbs(dbPage);
        dto.setMealFoodMakeDbs(makeFoods);
        return dto;
    }

    // 대시보드 운동
    public AdminDashBoardExerciseDto getExerciseDashBoard(){
        AdminDashBoardExerciseDto dto = new AdminDashBoardExerciseDto();

        // 총 운동기록 수
        int totalRecordCount = exerciseRecordRepository.countTotalRecord();
        // 이번 주 기록 유저 수
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime startOfWeek = monday.atStartOfDay();
        int weeklyRecordUserCount = exerciseRecordRepository.countWeeklyRecordUser(startOfWeek);
        // 일 평균 운동 시간
        Double dailyExerciseAvg = adminMapper.getDailyExerciseAvg();

        dto.setTotalRecordCount(totalRecordCount);
        dto.setWeeklyRecordUserCount(weeklyRecordUserCount);
        dto.setDailyExerciseAverage(dailyExerciseAvg);

        return dto;
    }

    // 대시보드 식단
    public AdminDashBoardMealDto getMealDashBoard(){
        AdminDashBoardMealDto dto = new AdminDashBoardMealDto();

        // 총 식단기록 수
        int totalRecordCount = mealRecordDetailRepository.countAllMealRecord();
        // 이번 주 기록 유저 수
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        int weeklyRecordUserCount = mealRecordDetailRepository.countWeeklyRecordUser(monday);
        // 1인당 평균 칼로리 섭취
        Double calorieAvg = mealRecordDetailRepository.getCalorieAvg();
        calorieAvg = calorieAvg != null ? Math.round(calorieAvg * 10) / 10.0 : 0.0;

        dto.setTotalRecordCount(totalRecordCount);
        dto.setWeeklyRecordUserCount(weeklyRecordUserCount);
        dto.setCalorieAverage(calorieAvg);

        return dto;
    }

    // 통계 커뮤니티
    public AdminStatisticsCommunityDto getCommunityStatistics(){
        AdminStatisticsCommunityDto dto = new AdminStatisticsCommunityDto();

        // 카테고리별 게시글 수
        List<CategoryPostCountRes> categoryPostCount = communityPostRepository.countPostByCategory();
        // 6개월간 게시글 추이
        List<PostCountRes> postCount = adminMapper.countByPost();

        dto.setCategoryPostCount(categoryPostCount);
        dto.setPostCount(postCount);

        return dto;
    }

    // 통계 운동
    public AdminStatisticsExerciseDto getExerciseStatistics(){
        AdminStatisticsExerciseDto dto = new AdminStatisticsExerciseDto();

        // 6개월간 운동기록 추이
        List<ExerciseRecordCountRes> exerciseRecordCount = adminMapper.countByExerciseRecord();
        // 운동종목별 기록 수
        List<ExerciseNameRecordCountRes> exerciseNameRecordCount = adminMapper.countByExerciseName();
        // 시간대별 운동 분포
        List<ExerciseDistributionCountRes> exerciseDistributionCount = adminMapper.countExerciseTimeDistribution();

        dto.setExerciseRecordCountResList(exerciseRecordCount);
        dto.setExerciseNameRecordCountResList(exerciseNameRecordCount);
        dto.setExerciseDistributionCountResList(exerciseDistributionCount);

        return dto;
    }

    // 통계 식단
    public AdminStatisticsMealDto getMealStatistics(){
        AdminStatisticsMealDto dto = new AdminStatisticsMealDto();

        // 6개월간 식단기록 추이
        List<MealRecordCountRes> mealRecordCount = adminMapper.countByMealRecord();
        // 탄단지 평균 섭취량
        MealMacroAverageRes mealMacroAvg = adminMapper.getMacroAvg();

        dto.setMealRecordCount(mealRecordCount);
        dto.setMealMacroAverage(mealMacroAvg);

        return dto;
    }
    public void removeUser(Long userId) {
        try {
            // 운동 기록 삭제
            exerciseRecordRepository.deleteAllByUserId(userId);

            // 식단 기록 삭제
            mealRecordDetailRepository.deleteAllByUserId(userId);

            // 물 섭취 기록 삭제
            dailyWaterIntakeRepository.deleteAllByUserId(userId);

            // 댓글 삭제
            mentRepository.deleteAllByUserId(userId);
            // 좋아요 삭제
            communityLikeRepository.deleteAllByUserId(userId);
            // 게시글 파일 삭제
            communityPostFileRepository.deleteAllByUserId(userId);
            // 게시글 삭제(소프트 딜리트)
            int result = communityPostRepository.softDeleteByUserId(userId);
            log.info("Life 서버: 유저 {} 관련 데이터 삭제 완료", userId);
            log.info("Life 게시글 삭제 수 : {}", result);
        } catch (Exception e) {
            log.error("Life 서버: 유저 {} 데이터 삭제 중 오류 발생", userId, e);
            throw e; // controller에서 catch해서 실패 Response 내려줌
        }
    }

    public AdminCommunityDataDto getCommunityData(Long postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글 삭제 됨"));
        List<AdminCommunityDataDto.CommentDto> comments =
                mentRepository.findByPostPostId(postId).stream()
                        .map(c -> new AdminCommunityDataDto.CommentDto(c.getCommentId(), c.getContent(),c.getNickName(), c.getCreatedAt()))
                        .toList();

        List<AdminCommunityDataDto.FileDto> files =
                communityPostFileRepository.findByPostPostId(postId).stream()
                        .map(f -> new AdminCommunityDataDto.FileDto(f.getId(), f.getFilePath()))
                        .toList();

        return AdminCommunityDataDto.builder()
                .nickName(post.getNickName())
                .title(post.getTitle())
                .category(post.getCategory().getName())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .isDeleted(post.getIsDeleted())
                .createdAt(post.getCreatedAt())
                .comments(comments)
                .files(files)
                .build();
    }


    public ResultResponse<?> removeCommunity(Long postId) {
        int postResult = communityPostRepository.softDeleteByPostId(postId);
//        int likeCount = communityLikeRepository.deleteByPostId(postId);
//        int fileCount = communityPostFileRepository.deleteByPostId(postId);
//        int commentCount = mentRepository.deleteByPostId(postId);

        return new ResultResponse<>("게시글 삭제 완료", postResult);
    }
}
