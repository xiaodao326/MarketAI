package com.marketai.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marketai.backend.dto.insight.CreateInsightRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.vo.insight.CreateInsightResponse;
import com.marketai.backend.vo.insight.InsightReportSummaryVO;
import com.marketai.backend.vo.insight.InsightReportVO;
import com.marketai.backend.vo.insight.TaskStatusVO;

public interface InsightService {

    /** 创建分析任务，立即返回 taskId，后台异步处理 */
    CreateInsightResponse createTask(CreateInsightRequest request, User user);

    /** 查询任务状态 */
    TaskStatusVO getTaskStatus(Long taskId, User user);

    /** 获取完整洞察报告 */
    InsightReportVO getReport(Long reportId, User user);

    /** 分页查询项目历史报告 */
    Page<InsightReportSummaryVO> listProjectReports(Long projectId, int page, int size, User user);

    /** 消费者调用：执行任务分析（更新 DB 状态、调用 AI、保存报告）*/
    void processTask(Long taskId);
}
