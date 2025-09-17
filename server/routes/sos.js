const express = require('express');
const router = express.Router();

// 임시 SOS 알림 저장소 (실제로는 데이터베이스 사용)
let sosAlerts = [];

// SOS 알림 전송
router.post('/', (req, res) => {
    try {
        const {
            userId,
            userName,
            latitude,
            longitude,
            location,
            message,
            emergencyType
        } = req.body;
        
        const alert = {
            alertId: Date.now().toString(),
            userId,
            userName,
            latitude,
            longitude,
            location,
            message,
            emergencyType,
            timestamp: new Date().toISOString(),
            status: 'ACTIVE',
            responseCount: 0
        };
        
        sosAlerts.push(alert);
        
        res.status(201).json({
            success: true,
            message: 'SOS 알림이 성공적으로 전송되었습니다.',
            data: alert
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: 'SOS 알림 전송 중 오류가 발생했습니다.'
        });
    }
});

// 주변 SOS 알림 가져오기
router.get('/nearby', (req, res) => {
    try {
        const { latitude, longitude, radius = 5 } = req.query;
        
        const nearbyAlerts = sosAlerts.filter(alert => {
            if (alert.status !== 'ACTIVE') return false;
            
            // 간단한 거리 계산 (실제로는 Haversine 공식 사용)
            const distance = Math.sqrt(
                Math.pow(alert.latitude - parseFloat(latitude), 2) +
                Math.pow(alert.longitude - parseFloat(longitude), 2)
            ) * 111; // 대략적인 km 변환
            
            return distance <= parseFloat(radius);
        });
        
        res.json({
            success: true,
            data: nearbyAlerts,
            count: nearbyAlerts.length
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '주변 SOS 알림을 가져오는 중 오류가 발생했습니다.'
        });
    }
});

// SOS 알림 응답
router.post('/:alertId/respond', (req, res) => {
    try {
        const { alertId } = req.params;
        const { responderId, responderName, message } = req.body;
        
        const alert = sosAlerts.find(a => a.alertId === alertId);
        if (!alert) {
            return res.status(404).json({
                success: false,
                error: '해당 SOS 알림을 찾을 수 없습니다.'
            });
        }
        
        alert.responseCount += 1;
        
        res.json({
            success: true,
            message: 'SOS 알림에 응답했습니다.',
            data: { alertId, responseCount: alert.responseCount }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: 'SOS 알림 응답 중 오류가 발생했습니다.'
        });
    }
});

module.exports = router; 