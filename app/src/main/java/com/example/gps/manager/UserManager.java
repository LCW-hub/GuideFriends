package com.example.gps.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_COINS = "coins";
    private static final String KEY_TOTAL_STEPS = "total_steps";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private static UserManager instance;
    private Context context;
    private SharedPreferences prefs;
    private User currentUser;

    private UserManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadUserData();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    /**
     * 로그인 처리
     */
    public void login(String username, String password, LoginCallback callback) {
        // 로컬 테스트 모드 (서버 없이 테스트)
        if (isLocalTestMode()) {
            // 테스트 계정들
            if (isValidTestAccount(username, password)) {
                Map<String, Object> userData = getTestUserData(username);
                saveUserData(userData);
                callback.onSuccess("로그인 성공 (로컬 모드)");
                return;
            } else {
                callback.onError("사용자명 또는 비밀번호가 올바르지 않습니다");
                return;
            }
        }

        // 서버 모드
        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        User user = new User(username, password, "", "");

        Call<Map<String, Object>> call = userApi.login(user);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if ("success".equals(result.get("status"))) {
                        Map<String, Object> userData = (Map<String, Object>) result.get("user");
                        saveUserData(userData);
                        callback.onSuccess("로그인 성공");
                    } else {
                        callback.onError(result.get("message").toString());
                    }
                } else {
                    callback.onError("서버 오류가 발생했습니다");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "로그인 실패", t);
                // 서버 연결 실패 시 로컬 모드로 전환
                if (isValidTestAccount(username, password)) {
                    Map<String, Object> userData = getTestUserData(username);
                    saveUserData(userData);
                    callback.onSuccess("로그인 성공 (로컬 모드)");
                } else {
                    callback.onError("네트워크 오류가 발생했습니다");
                }
            }
        });
    }

    /**
     * 회원가입 처리
     */
    public void signup(String username, String password, String email, String name, SignupCallback callback) {
        // 로컬 테스트 모드
        if (isLocalTestMode()) {
            // 간단한 검증
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty()) {
                callback.onError("모든 필드를 입력해주세요");
                return;
            }
            
            // 새 사용자 데이터 생성
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", System.currentTimeMillis() % 10000); // 임시 ID
            userData.put("username", username);
            userData.put("email", email);
            userData.put("name", name);
            userData.put("coins", 1000); // 가입 시 기본 코인
            userData.put("totalSteps", 0);
            
            saveUserData(userData);
            callback.onSuccess("회원가입이 완료되었습니다 (로컬 모드)");
            return;
        }

        // 서버 모드
        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        User user = new User(username, password, email, name);

        Call<Map<String, Object>> call = userApi.signup(user);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if ("success".equals(result.get("status"))) {
                        Map<String, Object> userData = (Map<String, Object>) result.get("user");
                        saveUserData(userData);
                        callback.onSuccess("회원가입이 완료되었습니다");
                    } else {
                        callback.onError(result.get("message").toString());
                    }
                } else {
                    callback.onError("서버 오류가 발생했습니다");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "회원가입 실패", t);
                callback.onError("네트워크 오류가 발생했습니다");
            }
        });
    }

    /**
     * 로그아웃 처리
     */
    public void logout() {
        prefs.edit().clear().apply();
        currentUser = null;
    }

    /**
     * 사용자 데이터 저장
     */
    private void saveUserData(Map<String, Object> userData) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, ((Double) userData.get("id")).intValue());
        editor.putString(KEY_USERNAME, (String) userData.get("username"));
        editor.putString(KEY_EMAIL, (String) userData.get("email"));
        editor.putString(KEY_NAME, (String) userData.get("name"));
        editor.putInt(KEY_COINS, ((Double) userData.get("coins")).intValue());
        editor.putInt(KEY_TOTAL_STEPS, ((Double) userData.get("totalSteps")).intValue());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();

        // 현재 사용자 객체 업데이트
        currentUser = new User(
            (String) userData.get("username"),
            "",
            (String) userData.get("email"),
            (String) userData.get("name")
        );
    }

    /**
     * 저장된 사용자 데이터 로드
     */
    private void loadUserData() {
        if (isLoggedIn()) {
            currentUser = new User(
                prefs.getString(KEY_USERNAME, ""),
                "",
                prefs.getString(KEY_EMAIL, ""),
                prefs.getString(KEY_NAME, "")
            );
        }
    }

    /**
     * 로그인 상태 확인
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * 현재 사용자 정보 반환
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * 사용자 ID 반환
     */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * 사용자명 반환
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    /**
     * 현재 코인 수 반환
     */
    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    /**
     * 총 걸음수 반환
     */
    public int getTotalSteps() {
        return prefs.getInt(KEY_TOTAL_STEPS, 0);
    }

    /**
     * 코인 업데이트
     */
    public void updateCoins(int newCoins, CoinUpdateCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("로그인이 필요합니다");
            return;
        }

        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        Map<String, Integer> coinData = new HashMap<>();
        coinData.put("coins", newCoins);
        Call<Map<String, Object>> call = userApi.updateCoins(getUserId(), coinData);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if ("success".equals(result.get("status"))) {
                        // 로컬 데이터 업데이트
                        prefs.edit().putInt(KEY_COINS, newCoins).apply();
                        callback.onSuccess(newCoins);
                    } else {
                        callback.onError(result.get("message").toString());
                    }
                } else {
                    callback.onError("서버 오류가 발생했습니다");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "코인 업데이트 실패", t);
                callback.onError("네트워크 오류가 발생했습니다");
            }
        });
    }

    /**
     * 만보기 보상 요청
     */
    public void requestStepReward(int steps, StepRewardCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("로그인이 필요합니다");
            return;
        }

        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        Map<String, Object> stepData = new HashMap<>();
        stepData.put("steps", steps);
        stepData.put("date", new Date().toString());
        Call<Map<String, Object>> call = userApi.requestStepReward(getUserId(), stepData);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if ("success".equals(result.get("status"))) {
                        Map<String, Object> reward = (Map<String, Object>) result.get("reward");
                        int newCoins = ((Double) reward.get("totalCoins")).intValue();
                        int rewardCoins = ((Double) reward.get("coins")).intValue();
                        
                        // 로컬 데이터 업데이트
                        prefs.edit()
                            .putInt(KEY_COINS, newCoins)
                            .putInt(KEY_TOTAL_STEPS, getTotalSteps() + steps)
                            .apply();
                        
                        callback.onSuccess(rewardCoins, newCoins, result.get("message").toString());
                    } else {
                        callback.onError(result.get("message").toString());
                    }
                } else {
                    callback.onError("서버 오류가 발생했습니다");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "만보기 보상 요청 실패", t);
                callback.onError("네트워크 오류가 발생했습니다");
            }
        });
    }

    // 콜백 인터페이스들
    public interface LoginCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface SignupCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface CoinUpdateCallback {
        void onSuccess(int newCoins);
        void onError(String error);
    }

    public interface StepRewardCallback {
        void onSuccess(int rewardCoins, int totalCoins, String message);
        void onError(String error);
    }

    // 로컬 테스트 모드 관련 메서드들
    private boolean isLocalTestMode() {
        // 서버 연결을 시도해보고 실패하면 로컬 모드로 전환
        return true; // 일단 항상 로컬 모드로 설정
    }

    private boolean isValidTestAccount(String username, String password) {
        return (username.equals("testuser") && password.equals("test123")) ||
               (username.equals("walkinglover") && password.equals("walk123")) ||
               (username.equals("stepmaster") && password.equals("step456"));
    }

    private Map<String, Object> getTestUserData(String username) {
        Map<String, Object> userData = new HashMap<>();
        
        switch (username) {
            case "testuser":
                userData.put("id", 1);
                userData.put("username", "testuser");
                userData.put("email", "test@example.com");
                userData.put("name", "테스트 사용자");
                userData.put("coins", 1000);
                userData.put("totalSteps", 0);
                break;
            case "walkinglover":
                userData.put("id", 2);
                userData.put("username", "walkinglover");
                userData.put("email", "walking@example.com");
                userData.put("name", "산책 애호가");
                userData.put("coins", 1500);
                userData.put("totalSteps", 2500);
                break;
            case "stepmaster":
                userData.put("id", 3);
                userData.put("username", "stepmaster");
                userData.put("email", "step@example.com");
                userData.put("name", "만보기 마스터");
                userData.put("coins", 2000);
                userData.put("totalSteps", 5000);
                break;
        }
        
        return userData;
    }
}
