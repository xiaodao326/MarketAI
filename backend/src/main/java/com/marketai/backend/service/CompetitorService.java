package com.marketai.backend.service;

import com.marketai.backend.dto.competitor.AnalyzeCompetitorsRequest;
import com.marketai.backend.dto.competitor.UpdateCompetitorRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.vo.competitor.AnalyzeCompetitorsResponse;
import com.marketai.backend.vo.competitor.CompetitorTaskStatusVO;
import com.marketai.backend.vo.competitor.CompetitorVO;
import com.marketai.backend.vo.competitor.DifferentiationInsightVO;
import com.marketai.backend.vo.competitor.FeatureMatrixVO;

import java.util.List;

public interface CompetitorService {

    /** 触发 AI 分析任务,立即返回 taskId */
    AnalyzeCompetitorsResponse createTask(AnalyzeCompetitorsRequest request, User user);

    /** 查询任务状态 */
    CompetitorTaskStatusVO getTaskStatus(Long taskId, User user);

    /** 项目下所有竞品 */
    List<CompetitorVO> listProjectCompetitors(Long projectId, User user);

    /** 单个竞品 */
    CompetitorVO getCompetitor(Long id, User user);

    /** 更新竞品 (部分字段) */
    CompetitorVO updateCompetitor(Long id, UpdateCompetitorRequest request, User user);

    /** 项目的功能对比矩阵 */
    FeatureMatrixVO getFeatureMatrix(Long projectId, User user);

    /** 项目的差异化建议 */
    List<DifferentiationInsightVO> getInsights(Long projectId, User user);

    /** 消费者调用: 执行分析任务 */
    void processTask(Long taskId);
}
