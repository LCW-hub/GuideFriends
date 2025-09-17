const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// 미들웨어 설정
app.use(helmet());
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Rate limiting
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15분
    max: 100 // IP당 최대 100개 요청
});
app.use(limiter);

// 라우트 설정
app.use('/api/courses', require('./routes/courses'));
app.use('/api/sos', require('./routes/sos'));
app.use('/api/transport', require('./routes/transport'));

// 기본 라우트
app.get('/', (req, res) => {
    res.json({ 
        message: 'GPS Walking App API Server',
        version: '1.0.0',
        status: 'running'
    });
});

// 에러 핸들링
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ 
        error: '서버 내부 오류가 발생했습니다.',
        message: err.message 
    });
});

// 404 핸들링
app.use('*', (req, res) => {
    res.status(404).json({ 
        error: '요청한 API 엔드포인트를 찾을 수 없습니다.' 
    });
});

app.listen(PORT, () => {
    console.log(`🚀 서버가 포트 ${PORT}에서 실행 중입니다.`);
    console.log(`📱 API 엔드포인트: http://localhost:${PORT}/api`);
}); 