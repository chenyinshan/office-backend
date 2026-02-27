package com.example.oa.workflow.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 解析 wf_node 的 next_node_keys、approver_config 等 JSON 字段。
 */
public final class WfJsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WfJsonUtil() {}

    /** next_node_keys 如 ["dept_leader"] -> List */
    public static List<String> parseNextNodeKeys(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** approver_config 如 {"assignee_user_id": 2} -> 取 assignee_user_id */
    public static Long parseAssigneeUserId(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> map = MAPPER.readValue(json, new TypeReference<>() {});
            Object v = map.get("assignee_user_id");
            if (v == null) return null;
            if (v instanceof Number) return ((Number) v).longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
