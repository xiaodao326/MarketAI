package com.marketai.backend.service;

import com.marketai.backend.ai.persona.dto.PersonaGenerateRequest;
import com.marketai.backend.dto.persona.CreatePersonaRequest;
import com.marketai.backend.dto.persona.UpdatePersonaRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.vo.persona.CreatePersonaTaskResponse;
import com.marketai.backend.vo.persona.PersonaTaskStatusVO;
import com.marketai.backend.vo.persona.PersonaVO;

import java.util.List;

public interface PersonaService {

    /** 创建画像生成任务, 立即返回 taskId, 后台异步执行 */
    CreatePersonaTaskResponse createTask(PersonaGenerateRequest request, User user);

    /** 查询任务状态 */
    PersonaTaskStatusVO getTaskStatus(Long taskId, User user);

    /** 获取项目的全部画像 (核心用户置顶, 然后按 market_share 降序) */
    List<PersonaVO> listProjectPersonas(Long projectId, User user);

    /** 获取单个画像 */
    PersonaVO getPersona(Long id, User user);

    /** 手动创建画像 */
    PersonaVO createPersona(CreatePersonaRequest request, User user);

    /** 更新画像 (部分字段) */
    PersonaVO updatePersona(Long id, UpdatePersonaRequest request, User user);

    /** 删除画像 */
    void deletePersona(Long id, User user);

    /** 消费者调用: 执行生成任务 */
    void processTask(Long taskId);
}
