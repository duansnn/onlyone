package com.owner.system.kanban.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务新增/编辑入参（更新时仅覆盖非空字段，拖拽改状态也走它）。
 * <p>新增时 title 必填（服务端校验），status 缺省 todo，priority 缺省 medium。
 * 继承 {@link TaskImportItem} 以复用字段校验与批量落库逻辑。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskSaveDTO extends TaskImportItem {
}
