const express = require('express');
const router = express.Router();

// 메모리 기반 사용자 데이터베이스 (실제 프로덕션에서는 MongoDB 등 사용)
let users = [
    {
        id: 1,
        username: 'testuser',
        password: 'test123', // 실제로는 해시화해야 함
        email: 'test@example.com',
        name: '테스트 사용자',
        coins: 1000,
        totalSteps: 0,
        lastStepRewardDate: null,
        createdAt: new Date()
    },
    {
        id: 2,
        username: 'walkinglover',
        password: 'walk123',
        email: 'walking@example.com',
        name: '산책 애호가',
        coins: 1500,
        totalSteps: 2500,
        lastStepRewardDate: null,
        createdAt: new Date()
    },
    {
        id: 3,
        username: 'stepmaster',
        password: 'step456',
        email: 'step@example.com',
        name: '만보기 마스터',
        coins: 2000,
        totalSteps: 5000,
        lastStepRewardDate: null,
        createdAt: new Date()
    }
];

// 회원가입
router.post('/signup', (req, res) => {
    try {
        const { username, password, email, name } = req.body;

        // 입력 검증
        if (!username || !password || !email || !name) {
            return res.status(400).json({
                status: 'error',
                message: '모든 필드를 입력해주세요.'
            });
        }

        // 중복 사용자명 확인
        const existingUser = users.find(user => user.username === username);
        if (existingUser) {
            return res.status(400).json({
                status: 'error',
                message: '이미 존재하는 사용자명입니다.'
            });
        }

        // 새 사용자 생성
        const newUser = {
            id: users.length + 1,
            username,
            password, // 실제로는 bcrypt로 해시화해야 함
            email,
            name,
            coins: 1000, // 가입 시 기본 코인 1000개 지급
            totalSteps: 0,
            lastStepRewardDate: null,
            createdAt: new Date()
        };

        users.push(newUser);

        res.status(201).json({
            status: 'success',
            message: '회원가입이 완료되었습니다.',
            user: {
                id: newUser.id,
                username: newUser.username,
                email: newUser.email,
                name: newUser.name,
                coins: newUser.coins
            }
        });

    } catch (error) {
        console.error('회원가입 오류:', error);
        res.status(500).json({
            status: 'error',
            message: '서버 오류가 발생했습니다.'
        });
    }
});

// 로그인
router.post('/login', (req, res) => {
    try {
        const { username, password } = req.body;

        // 입력 검증
        if (!username || !password) {
            return res.status(400).json({
                status: 'error',
                message: '사용자명과 비밀번호를 입력해주세요.'
            });
        }

        // 사용자 찾기
        const user = users.find(u => u.username === username && u.password === password);
        if (!user) {
            return res.status(401).json({
                status: 'error',
                message: '사용자명 또는 비밀번호가 올바르지 않습니다.'
            });
        }

        res.json({
            status: 'success',
            message: '로그인 성공',
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                name: user.name,
                coins: user.coins,
                totalSteps: user.totalSteps
            }
        });

    } catch (error) {
        console.error('로그인 오류:', error);
        res.status(500).json({
            status: 'error',
            message: '서버 오류가 발생했습니다.'
        });
    }
});

// 사용자 정보 조회
router.get('/:userId', (req, res) => {
    try {
        const userId = parseInt(req.params.userId);
        const user = users.find(u => u.id === userId);

        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: '사용자를 찾을 수 없습니다.'
            });
        }

        res.json({
            status: 'success',
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                name: user.name,
                coins: user.coins,
                totalSteps: user.totalSteps
            }
        });

    } catch (error) {
        console.error('사용자 조회 오류:', error);
        res.status(500).json({
            status: 'error',
            message: '서버 오류가 발생했습니다.'
        });
    }
});

// 코인 업데이트
router.put('/:userId/coins', (req, res) => {
    try {
        const userId = parseInt(req.params.userId);
        const { coins } = req.body;

        const user = users.find(u => u.id === userId);
        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: '사용자를 찾을 수 없습니다.'
            });
        }

        user.coins = coins;

        res.json({
            status: 'success',
            message: '코인이 업데이트되었습니다.',
            coins: user.coins
        });

    } catch (error) {
        console.error('코인 업데이트 오류:', error);
        res.status(500).json({
            status: 'error',
            message: '서버 오류가 발생했습니다.'
        });
    }
});

// 만보기 보상 처리
router.post('/:userId/step-reward', (req, res) => {
    try {
        const userId = parseInt(req.params.userId);
        const { steps, date } = req.body;

        const user = users.find(u => u.id === userId);
        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: '사용자를 찾을 수 없습니다.'
            });
        }

        // 오늘 이미 보상을 받았는지 확인
        const today = new Date().toDateString();
        if (user.lastStepRewardDate === today) {
            return res.status(400).json({
                status: 'error',
                message: '오늘은 이미 보상을 받았습니다.'
            });
        }

        // 걸음수에 따른 코인 보상 계산 (1000걸음당 10코인)
        const coinReward = Math.floor(steps / 1000) * 10;
        
        if (coinReward > 0) {
            user.coins += coinReward;
            user.totalSteps += steps;
            user.lastStepRewardDate = today;

            res.json({
                status: 'success',
                message: `${steps}걸음으로 ${coinReward}코인을 획득했습니다!`,
                reward: {
                    steps: steps,
                    coins: coinReward,
                    totalCoins: user.coins
                }
            });
        } else {
            res.json({
                status: 'success',
                message: '1000걸음 이상 걸어야 코인을 받을 수 있습니다.',
                reward: {
                    steps: steps,
                    coins: 0,
                    totalCoins: user.coins
                }
            });
        }

    } catch (error) {
        console.error('만보기 보상 오류:', error);
        res.status(500).json({
            status: 'error',
            message: '서버 오류가 발생했습니다.'
        });
    }
});

// 모든 사용자 목록 조회 (관리자용)
router.get('/', (req, res) => {
    try {
        const userList = users.map(user => ({
            id: user.id,
            username: user.username,
            email: user.email,
            name: user.name,
            coins: user.coins,
            totalSteps: user.totalSteps,
            createdAt: user.createdAt
        }));

        res.json({
            status: 'success',
            users: userList
        });

    } catch (error) {
        console.error('사용자 목록 조회 오류:', error);
        res.status(500).json({
            status: 'error',
            message: '서버 오류가 발생했습니다.'
        });
    }
});

module.exports = router;
