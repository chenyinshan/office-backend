package com.example.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.NoticeVo;
import com.example.oa.user.entity.OaNotice;
import com.example.oa.user.entity.OaNoticeRead;
import com.example.oa.user.entity.SysUserAccount;
import com.example.oa.user.mapper.OaNoticeMapper;
import com.example.oa.user.mapper.OaNoticeReadMapper;
import com.example.oa.user.mapper.SysUserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final OaNoticeMapper noticeMapper;
    private final OaNoticeReadMapper readMapper;
    private final NotificationService notificationService;
    private final SysUserAccountMapper userAccountMapper;

    /** 分页列表：已发布的，置顶优先再按发布时间倒序 */
    public List<NoticeVo> listPublished(Long currentUserId) {
        List<OaNotice> list = noticeMapper.selectList(
                Wrappers.<OaNotice>lambdaQuery()
                        .eq(OaNotice::getStatus, "published")
                        .orderByDesc(OaNotice::getIsTop)
                        .orderByDesc(OaNotice::getPublishAt));
        Set<Long> readIds = readMapper.selectList(
                        Wrappers.<OaNoticeRead>lambdaQuery().eq(OaNoticeRead::getUserId, currentUserId))
                .stream().map(OaNoticeRead::getNoticeId).collect(Collectors.toSet());
        return list.stream()
                .map(n -> NoticeVo.fromEntity(n, readIds.contains(n.getId())))
                .collect(Collectors.toList());
    }

    /** 详情；若 markRead 则记录已读。已发布所有人可看；草稿仅作者可看 */
    public NoticeVo getById(Long id, Long currentUserId, boolean markRead) {
        OaNotice n = noticeMapper.selectById(id);
        if (n == null) throw new BizException(ResultCode.NOT_FOUND, "公告不存在");
        if ("draft".equals(n.getStatus())) {
            if (!n.getPublisherUserId().equals(currentUserId)) throw new BizException(ResultCode.NOT_FOUND, "公告不存在");
            return NoticeVo.fromEntity(n, false);
        }
        if (!"published".equals(n.getStatus())) throw new BizException(ResultCode.NOT_FOUND, "公告未发布");

        boolean read = readMapper.selectCount(
                Wrappers.<OaNoticeRead>lambdaQuery()
                        .eq(OaNoticeRead::getNoticeId, id)
                        .eq(OaNoticeRead::getUserId, currentUserId)) > 0;
        if (markRead && !read) {
            OaNoticeRead r = new OaNoticeRead();
            r.setNoticeId(id);
            r.setUserId(currentUserId);
            r.setReadAt(LocalDateTime.now());
        readMapper.insert(r);
        read = true;
        }
        return NoticeVo.fromEntity(n, read);
    }

    /** 发布公告后给全员发站内通知 */
    private void notifyNoticePublished(Long noticeId, String title, Long publisherUserId) {
        List<Long> userIds = userAccountMapper.selectList(
                Wrappers.<SysUserAccount>lambdaQuery().select(SysUserAccount::getId))
                .stream().map(SysUserAccount::getId).collect(Collectors.toList());
        String noticeTitle = "新公告：" + (title != null ? title : "");
        for (Long uid : userIds) {
            notificationService.create(uid, "notice_published", noticeTitle, null, "notice", String.valueOf(noticeId));
        }
    }

    /** 下架公告后给全员发站内通知 */
    private void notifyNoticeUnpublished(Long noticeId, String title) {
        List<Long> userIds = userAccountMapper.selectList(
                Wrappers.<SysUserAccount>lambdaQuery().select(SysUserAccount::getId))
                .stream().map(SysUserAccount::getId).collect(Collectors.toList());
        String noticeTitle = "公告已下架：" + (title != null ? title : "");
        for (Long uid : userIds) {
            notificationService.create(uid, "notice_unpublished", noticeTitle, null, "notice", String.valueOf(noticeId));
        }
    }

    /** 发布公告（简化：有 userId 即可发布） */
    @Transactional(rollbackFor = Exception.class)
    public OaNotice publish(Long publisherUserId, String title, String content, boolean isTop) {
        OaNotice n = new OaNotice();
        n.setTitle(title);
        n.setContent(content);
        n.setPublisherUserId(publisherUserId);
        n.setIsTop(isTop != false ? 1 : 0);
        n.setStatus("published");
        n.setPublishAt(LocalDateTime.now());
        noticeMapper.insert(n);
        notifyNoticePublished(n.getId(), n.getTitle(), publisherUserId);
        return n;
    }

    /** 保存草稿（仅 ADMIN 可创建草稿） */
    @Transactional(rollbackFor = Exception.class)
    public OaNotice saveDraft(Long userId, String title, String content, boolean isTop) {
        OaNotice n = new OaNotice();
        n.setTitle(title != null ? title : "");
        n.setContent(content);
        n.setPublisherUserId(userId);
        n.setIsTop(isTop ? 1 : 0);
        n.setStatus("draft");
        n.setPublishAt(null);
        noticeMapper.insert(n);
        return n;
    }

    /** 当前用户创建的草稿列表（按更新时间倒序） */
    public List<NoticeVo> listMyDrafts(Long userId) {
        List<OaNotice> list = noticeMapper.selectList(
                Wrappers.<OaNotice>lambdaQuery()
                        .eq(OaNotice::getPublisherUserId, userId)
                        .eq(OaNotice::getStatus, "draft")
                        .orderByDesc(OaNotice::getUpdatedAt));
        return list.stream()
                .map(n -> NoticeVo.fromEntity(n, false))
                .collect(Collectors.toList());
    }

    /** 更新公告（草稿或已发布本人可改标题、内容、置顶） */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Long userId, String title, String content, Boolean isTop) {
        OaNotice n = noticeMapper.selectById(id);
        if (n == null) throw new BizException(ResultCode.NOT_FOUND, "公告不存在");
        if (!n.getPublisherUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN, "无权限修改");
        if (title != null) n.setTitle(title);
        if (content != null) n.setContent(content);
        if (isTop != null) n.setIsTop(isTop ? 1 : 0);
        noticeMapper.updateById(n);
    }

    /** 发布草稿（仅本人草稿可发布） */
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id, Long userId) {
        OaNotice n = noticeMapper.selectById(id);
        if (n == null) throw new BizException(ResultCode.NOT_FOUND, "公告不存在");
        if (!"draft".equals(n.getStatus())) throw new BizException(ResultCode.BAD_REQUEST, "仅草稿可发布");
        if (!n.getPublisherUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN, "无权限");
        n.setStatus("published");
        n.setPublishAt(LocalDateTime.now());
        noticeMapper.updateById(n);
        notifyNoticePublished(n.getId(), n.getTitle(), userId);
    }

    /** 下架公告（已发布的改为草稿状态，仅本人或 ADMIN 可下架） */
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id, Long userId, boolean isAdmin) {
        OaNotice n = noticeMapper.selectById(id);
        if (n == null) throw new BizException(ResultCode.NOT_FOUND, "公告不存在");
        if (!"published".equals(n.getStatus())) throw new BizException(ResultCode.BAD_REQUEST, "仅已发布可下架");
        if (!n.getPublisherUserId().equals(userId) && !isAdmin) throw new BizException(ResultCode.FORBIDDEN, "无权限");
        n.setStatus("draft");
        n.setPublishAt(null);
        noticeMapper.updateById(n);
        notifyNoticeUnpublished(n.getId(), n.getTitle());
    }

    /** 删除：草稿仅作者可删，已发布仅 ADMIN 可删 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId, boolean isAdmin) {
        OaNotice n = noticeMapper.selectById(id);
        if (n == null) throw new BizException(ResultCode.NOT_FOUND, "公告不存在");
        if ("draft".equals(n.getStatus())) {
            if (!n.getPublisherUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN, "无权限");
        } else {
            if (!isAdmin) throw new BizException(ResultCode.FORBIDDEN, "仅管理员可删除已发布公告");
        }
        noticeMapper.deleteById(id);
    }

    /** 标记已读 */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId, Long userId) {
        long c = readMapper.selectCount(
                Wrappers.<OaNoticeRead>lambdaQuery()
                        .eq(OaNoticeRead::getNoticeId, noticeId)
                        .eq(OaNoticeRead::getUserId, userId));
        if (c > 0) return;
        OaNoticeRead r = new OaNoticeRead();
        r.setNoticeId(noticeId);
        r.setUserId(userId);
        r.setReadAt(LocalDateTime.now());
        readMapper.insert(r);
    }
}
