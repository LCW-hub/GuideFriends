const express = require('express');
const router = express.Router();

// 임시 대중교통 데이터 (실제로는 외부 API 연동)
const transportData = {
    "삼성로타리": [
        {
            type: "버스",
            route: "101번",
            direction: "시내 방향",
            nextArrival: "3분 후",
            frequency: "10분 간격"
        },
        {
            type: "지하철",
            route: "1호선",
            direction: "동대구 방향",
            nextArrival: "5분 후",
            frequency: "8분 간격"
        }
    ],
    "북문": [
        {
            type: "버스",
            route: "203번",
            direction: "북구 방향",
            nextArrival: "2분 후",
            frequency: "15분 간격"
        }
    ]
};

// 특정 위치의 대중교통 정보 가져오기
router.get('/:location', (req, res) => {
    try {
        const { location } = req.params;
        
        const transport = transportData[location] || [];
        
        res.json({
            success: true,
            data: {
                location,
                transport,
                count: transport.length
            }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '대중교통 정보를 가져오는 중 오류가 발생했습니다.'
        });
    }
});

// 주변 대중교통 정보 검색
router.get('/nearby/search', (req, res) => {
    try {
        const { latitude, longitude, radius = 1 } = req.query;
        
        // 간단한 구현 (실제로는 지리적 쿼리 필요)
        const nearbyTransport = Object.keys(transportData).map(location => ({
            location,
            distance: (Math.random() * 2).toFixed(2), // 임시 거리
            transport: transportData[location]
        }));
        
        res.json({
            success: true,
            data: nearbyTransport,
            searchParams: { latitude, longitude, radius }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '주변 대중교통 정보를 검색하는 중 오류가 발생했습니다.'
        });
    }
});

module.exports = router; 