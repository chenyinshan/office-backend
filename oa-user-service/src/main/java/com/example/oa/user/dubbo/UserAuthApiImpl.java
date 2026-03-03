package com.example.oa.user.dubbo;

import com.example.oa.api.IUserAuthApi;
import com.example.oa.api.dto.UserInfoDTO;
import com.example.oa.user.dto.UserInfoVo;
import com.example.oa.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户认证 Dubbo 服务实现：根据 token 返回当前用户信息，供 portal 等通过 Dubbo 调用。
 */
@DubboService
@RequiredArgsConstructor
public class UserAuthApiImpl implements IUserAuthApi {

    private final AuthService authService;

    @Override
    public UserInfoDTO getCurrentUser(String token) {
        UserInfoVo vo = authService.getCurrentUser(token);
        if (vo == null) {
            return null;
        }
        return toDTO(vo);
    }

    private static UserInfoDTO toDTO(UserInfoVo vo) {
        List<String> roles = vo.getRoles() != null ? new ArrayList<>(vo.getRoles()) : new ArrayList<>();
        return UserInfoDTO.builder()
                .userId(vo.getUserId())
                .username(vo.getUsername())
                .employeeId(vo.getEmployeeId())
                .employeeNo(vo.getEmployeeNo())
                .name(vo.getName())
                .deptId(vo.getDeptId())
                .deptName(vo.getDeptName())
                .postId(vo.getPostId())
                .postName(vo.getPostName())
                .roles(roles)
                .build();
    }
}
