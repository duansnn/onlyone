import { useState } from 'react';
import { Button, Modal, Space, Typography, Upload } from 'antd';
import type { UploadProps } from 'antd';
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { kanbanTaskApi } from '@/api/kanban';
import { toast } from '@/utils/feedback';

const { Paragraph, Text } = Typography;

interface ImportExportPanelProps {
  open: boolean;
  projectId: string;
  projectName?: string;
  onClose: () => void;
  onImported?: () => void;
}

/** 任务导入/导出弹窗 */
export default function ImportExportPanel({
  open,
  projectId,
  projectName,
  onClose,
  onImported,
}: ImportExportPanelProps) {
  const [exporting, setExporting] = useState(false);
  const [importing, setImporting] = useState(false);

  const handleExport = async () => {
    setExporting(true);
    try {
      const data = await kanbanTaskApi.export(projectId);
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${data.projectName || 'project'}-tasks.json`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success('导出成功');
    } catch {
      // 错误已提示
    } finally {
      setExporting(false);
    }
  };

  const uploadProps: UploadProps = {
    accept: '.json',
    maxCount: 1,
    showUploadList: false,
    beforeUpload: async (file) => {
      setImporting(true);
      try {
        const count = await kanbanTaskApi.import(projectId, file);
        toast.success(`成功导入 ${count} 个任务`);
        onImported?.();
        onClose();
      } catch {
        // 错误已提示
      } finally {
        setImporting(false);
      }
      // 返回 false 阻止 Upload 自行上传
      return false;
    },
  };

  return (
    <Modal
      title="导入 / 导出"
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={480}
    >
      <div className="kb-io" style={{ marginTop: 16 }}>
        <div className="kb-io__section">
          <Text strong>导出任务 JSON</Text>
          <Paragraph type="secondary" style={{ margin: '6px 0 12px' }}>
            将项目「{projectName ?? '-'}」下全部任务导出为 JSON 文件备份。
          </Paragraph>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={handleExport}
          >
            导出 JSON
          </Button>
        </div>

        <div className="kb-io__divider" />

        <div className="kb-io__section">
          <Text strong>导入任务 JSON</Text>
          <Paragraph type="secondary" style={{ margin: '6px 0 12px' }}>
            上传 JSON 文件批量导入任务到当前项目。兼容导出的 {'{"tasks":[...]}'} 结构或裸数组。
          </Paragraph>
          <Upload {...uploadProps}>
            <Button icon={<UploadOutlined />} loading={importing}>
              选择 JSON 文件
            </Button>
          </Upload>
        </div>
      </div>
    </Modal>
  );
}
