package com.example.oa.portal.client;

import com.example.oa.api.IUserAuthApi;
import com.example.oa.api.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 根据 token 获取当前用户（userId、employeeId 等）。优先通过 Dubbo 调用 user-service，走 Nacos 发现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    @DubboReference
    private IUserAuthApi userAuthApi;

    /**
     * 用 token 通过 Dubbo 调 user-service 获取当前用户，成功时返回 userId，失败或未登录返回 null。
     */
    public Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            UserInfoDTO user = userAuthApi.getCurrentUser(token.trim());
            return user != null ? user.getUserId() : null;
        } catch (Exception e) {
            log.debug("Dubbo IUserAuthApi.getCurrentUser 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 同 getUserIdFromToken，并返回 employeeId（用于 X-Employee-Id）；无则用 userId 填充。
     */
    public long[] getUserIdAndEmployeeIdFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            UserInfoDTO user = userAuthApi.getCurrentUser(token.trim());
            if (user == null || user.getUserId() == null) return null;
            long employeeId = user.getEmployeeId() != null ? user.getEmployeeId() : user.getUserId();
            return new long[]{user.getUserId(), employeeId};
        } catch (Exception e) {
            log.debug("Dubbo IUserAuthApi.getCurrentUser 调用失败: {}", e.getMessage());
            return null;
        }
    }
}
