package com.example.oa.user.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 一次性工具：生成 BCrypt 密码哈希，用于初始化或重置脚本。
 * 运行：在 oa-user-service 目录下
 *   mvn exec:java -Dexec.mainClass="com.example.oa.user.util.PasswordHashUtil" -Dexec.args="123456"
 */
public class PasswordHashUtil {

    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123456";
        String hash = new BCryptPasswordEncoder().encode(raw);
        System.out.println("原始密码: " + raw);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println("SQL: UPDATE sys_user_account SET password_hash = '" + hash + "' WHERE username = 'admin';");
    }
}
