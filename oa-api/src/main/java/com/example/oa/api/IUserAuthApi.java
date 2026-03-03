package com.example.oa.api;

import com.example.oa.api.dto.UserInfoDTO;

/**
 * 用户认证 Dubbo 接口：根据 token 获取当前用户信息，供 portal 等调用方使用。
 */
public interface IUserAuthApi {

    /**
     * 根据 token 获取当前用户信息；token 无效或过期返回 null。
     *
     * @param token 登录后获得的 token（通常来自请求头）
     * @return 用户信息，未登录或 token 无效时返回 null
     */
    UserInfoDTO getCurrentUser(String token);
}
