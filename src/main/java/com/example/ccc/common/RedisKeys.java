package com.example.ccc.common;

public class RedisKeys {

    public static final String USER_TOKEN = "user:token:";

    public static final String USER_INFO = "user:info:";

    public static final String TASK_LIST = "task:list:";

    public static final String TASK_INFO = "task:info:";

    public static final String LOGIN_CODE = "login:code:";

    public static final String RATE_LIMIT = "rate:limit:";

    public static final String STATS = "stats:";

    public static final String NULL_CACHE = "null:cache:";

    public static final String LEADERBOARD = "leaderboard:score";

    public static final String LEADERBOARD_UPDATE_LOCK = "leaderboard:lock:update:";

    public static final String CHECKIN = "checkin:";

    public static final String CHECKIN_CONTINUOUS = "checkin:continuous:";

    public static final String SUBMISSION_LIKES = "submission:likes:";

    public static final String EXCELLENT_WORKS = "excellent:works";

    public static final String TASK_SLOTS = "task:slots:";

    public static final String TASK_GRAB_LOCK = "task:grab:lock:";

    public static final String TASK_GRAB_RECORD = "task:grab:record:";

    public static final long TOKEN_EXPIRE = 7200L;

    public static final long USER_INFO_EXPIRE = 1800L;

    public static final long TASK_LIST_EXPIRE = 300L;

    public static final long TASK_INFO_EXPIRE = 600L;

    public static final long NULL_CACHE_EXPIRE = 60L;
}
