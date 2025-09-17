const express = require('express');
const router = express.Router();

// 임시 데이터 (실제로는 데이터베이스 사용)
const courses = [
    {
        courseId: 1,
        courseName: "삼성로타리 코스",
        description: "도심 속에서 즐기는 산책로",
        distance: 2.5,
        duration: 30,
        steps: 3000,
        petFriendly: true,
        calories: 125.0,
        waterIntake: 0.5,
        difficulty: "쉬움",
        crowdLevel: 3,
        rating: 4.2,
        reviewCount: 156
    },
    {
        courseId: 2,
        courseName: "북문 코스",
        description: "역사적인 북문을 감상하며 걷는 코스",
        distance: 3.2,
        duration: 40,
        steps: 4000,
        petFriendly: false,
        calories: 160.0,
        waterIntake: 0.6,
        difficulty: "보통",
        crowdLevel: 2,
        rating: 4.5,
        reviewCount: 89
    }
];

// 모든 코스 목록 가져오기
router.get('/', (req, res) => {
    try {
        res.json({
            success: true,
            data: courses,
            count: courses.length
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '코스 목록을 가져오는 중 오류가 발생했습니다.'
        });
    }
});

// 특정 코스 상세 정보 가져오기
router.get('/:id', (req, res) => {
    try {
        const courseId = parseInt(req.params.id);
        const course = courses.find(c => c.courseId === courseId);
        
        if (!course) {
            return res.status(404).json({
                success: false,
                error: '해당 코스를 찾을 수 없습니다.'
            });
        }
        
        res.json({
            success: true,
            data: course
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '코스 정보를 가져오는 중 오류가 발생했습니다.'
        });
    }
});

// 추천 코스 목록 가져오기
router.get('/recommendations', (req, res) => {
    try {
        const { season, weather, crowdLevel } = req.query;
        
        // 간단한 추천 로직 (실제로는 더 복잡한 알고리즘 사용)
        const recommendations = courses.map(course => ({
            courseId: course.courseId,
            courseName: course.courseName,
            recommendationScore: Math.random() * 5, // 임시 점수
            reason: `${season}에 ${weather} 날씨에 적합한 코스입니다.`,
            season: season,
            weather: weather,
            crowdLevel: course.crowdLevel,
            isFestival: Math.random() > 0.7, // 30% 확률로 축제
            festivalName: Math.random() > 0.7 ? "봄꽃 축제" : null,
            festivalPeriod: Math.random() > 0.7 ? "3월 15일 - 4월 15일" : null
        }));
        
        // 점수순으로 정렬
        recommendations.sort((a, b) => b.recommendationScore - a.recommendationScore);
        
        res.json({
            success: true,
            data: recommendations.slice(0, 10), // 상위 10개만 반환
            filters: { season, weather, crowdLevel }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '추천 코스를 가져오는 중 오류가 발생했습니다.'
        });
    }
});

// 유동인구 정보 업데이트
router.put('/:id/crowd', (req, res) => {
    try {
        const courseId = parseInt(req.params.id);
        const { crowdLevel } = req.body;
        
        const course = courses.find(c => c.courseId === courseId);
        if (!course) {
            return res.status(404).json({
                success: false,
                error: '해당 코스를 찾을 수 없습니다.'
            });
        }
        
        course.crowdLevel = crowdLevel;
        
        res.json({
            success: true,
            message: '유동인구 정보가 업데이트되었습니다.',
            data: { courseId, crowdLevel }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '유동인구 정보 업데이트 중 오류가 발생했습니다.'
        });
    }
});

module.exports = router; 