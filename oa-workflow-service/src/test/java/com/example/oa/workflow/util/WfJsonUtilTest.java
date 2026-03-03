package com.example.oa.workflow.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流 JSON 解析工具 WfJsonUtil 的单元测试。
 */
class WfJsonUtilTest {

    @Test
    void parseNextNodeKeys_nullOrBlank_returnsEmpty() {
        assertTrue(WfJsonUtil.parseNextNodeKeys(null).isEmpty());
        assertTrue(WfJsonUtil.parseNextNodeKeys("").isEmpty());
        assertTrue(WfJsonUtil.parseNextNodeKeys("   ").isEmpty());
    }

    @Test
    void parseNextNodeKeys_validArray_returnsList() {
        List<String> list = WfJsonUtil.parseNextNodeKeys("[\"dept_leader\", \"hr\"]");
        assertEquals(2, list.size());
        assertEquals("dept_leader", list.get(0));
        assertEquals("hr", list.get(1));
    }

    @Test
    void parseNextNodeKeys_singleElement() {
        List<String> list = WfJsonUtil.parseNextNodeKeys("[\"end\"]");
        assertEquals(1, list.size());
        assertEquals("end", list.get(0));
    }

    @Test
    void parseNextNodeKeys_emptyArray_returnsEmpty() {
        List<String> list = WfJsonUtil.parseNextNodeKeys("[]");
        assertTrue(list.isEmpty());
    }

    @Test
    void parseNextNodeKeys_invalidJson_returnsEmpty() {
        assertTrue(WfJsonUtil.parseNextNodeKeys("not json").isEmpty());
        assertTrue(WfJsonUtil.parseNextNodeKeys("{}}").isEmpty());
    }

    @Test
    void parseAssigneeUserId_nullOrBlank_returnsNull() {
        assertNull(WfJsonUtil.parseAssigneeUserId(null));
        assertNull(WfJsonUtil.parseAssigneeUserId(""));
        assertNull(WfJsonUtil.parseAssigneeUserId("   "));
    }

    @Test
    void parseAssigneeUserId_emptyObject_returnsNull() {
        assertNull(WfJsonUtil.parseAssigneeUserId("{}"));
    }

    @Test
    void parseAssigneeUserId_missingKey_returnsNull() {
        assertNull(WfJsonUtil.parseAssigneeUserId("{\"other\": 1}"));
    }

    @Test
    void parseAssigneeUserId_numberValue_returnsLong() {
        assertEquals(2L, WfJsonUtil.parseAssigneeUserId("{\"assignee_user_id\": 2}"));
        assertEquals(100L, WfJsonUtil.parseAssigneeUserId("{\"assignee_user_id\": 100}"));
    }

    @Test
    void parseAssigneeUserId_stringNumber_returnsParsedLong() {
        assertEquals(3L, WfJsonUtil.parseAssigneeUserId("{\"assignee_user_id\": \"3\"}"));
    }

    @Test
    void parseAssigneeUserId_invalidJson_returnsNull() {
        assertNull(WfJsonUtil.parseAssigneeUserId("invalid"));
        assertNull(WfJsonUtil.parseAssigneeUserId("{"));
    }
}
