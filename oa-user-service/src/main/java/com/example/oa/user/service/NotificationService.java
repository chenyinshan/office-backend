package com.example.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.NotificationVo;
import com.example.oa.user.entity.OaNotification;
import com.example.oa.user.mapper.OaNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 站内通知：创建、列表、未读数、标记已读。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final OaNotificationMapper notificationMapper;

    /** 为指定用户创建一条通知（由 portal 在审批通过/驳回后调用） */
    @Transactional(rollbackFor = Exception.class)
    public OaNotification create(Long targetUserId, String type, String title, String content,
                                 String businessType, String businessId) {
        OaNotification n = new OaNotification();
        n.setUserId(targetUserId);
        n.setType(type != null ? type : "");
        n.setTitle(title != null ? title : "");
        n.setContent(content);
        n.setBusinessType(businessType);
        n.setBusinessId(businessId);
        n.setIsRead(0);
        notificationMapper.insert(n);
        return n;
    }

    /** 当前用户的通知列表，按创建时间倒序，最多 200 条 */
    public List<NotificationVo> listByUser(Long userId) {
        List<OaNotification> list = notificationMapper.selectList(
                Wrappers.<OaNotification>lambdaQuery()
                        .eq(OaNotification::getUserId, userId)
                        .orderByDesc(OaNotification::getCreatedAt)
                        .last("LIMIT 200"));
        return list.stream().map(NotificationVo::fromEntity).collect(Collectors.toList());
    }

    /** 当前用户未读数量 */
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                Wrappers.<OaNotification>lambdaQuery()
                        .eq(OaNotification::getUserId, userId)
                        .eq(OaNotification::getIsRead, 0));
    }

    /** 标记已读（仅本人可操作） */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId, Long userId) {
        OaNotification n = notificationMapper.selectById(notificationId);
        if (n == null) throw new BizException(ResultCode.NOT_FOUND, "通知不存在");
        if (!n.getUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN, "无权限");
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }
}
