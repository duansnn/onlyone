import { useState } from 'react';
import { Upload } from 'antd';
import { CameraOutlined, DeleteOutlined, LoadingOutlined, UserOutlined } from '@ant-design/icons';
import { uploadApi } from '@/api/mall';
import { toast } from '@/utils/feedback';
import SafeImage from './SafeImage';

interface AvatarUploaderProps {
  /** 由 Form.Item 注入 */
  value?: string;
  /** 由 Form.Item 注入 */
  onChange?: (value?: string) => void;
  /** 直径，默认 96 */
  size?: number;
  /** 底部说明 */
  tip?: string;
  disabled?: boolean;
}

const MAX_SIZE = 5 * 1024 * 1024;

/**
 * 圆形头像上传：点一下选图、上传、回填地址。
 *
 * <p>和商品图不同，头像只有一张，所以没有排序与设主图的概念；
 * 上传成功后直接覆盖旧值，再把旧图换成新的。落盘目录是 {@code avatar}。</p>
 */
export default function AvatarUploader({
  value,
  onChange,
  size = 96,
  tip = '支持 jpg / png，不超过 5 MB',
  disabled = false,
}: AvatarUploaderProps) {
  const [uploading, setUploading] = useState(false);

  const pick = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      toast.error('请选择图片文件');
      return false;
    }
    if (file.size > MAX_SIZE) {
      toast.error('头像不能超过 5 MB');
      return false;
    }
    setUploading(true);
    try {
      const url = await uploadApi.image(file, 'avatar');
      onChange?.(url);
      toast.success('头像已更新');
    } catch {
      /* 拦截器已提示 */
    } finally {
      setUploading(false);
    }
    // 阻止 antd 自己发请求，上传由我们调接口完成
    return false;
  };

  const box = { width: size, height: size };

  return (
    <div className="avatar-up">
      <div className="avatar-up__box" style={box}>
        {value ? (
          <SafeImage src={value} alt="头像" className="avatar-up__img" />
        ) : (
          <span className="avatar-up__placeholder">
            <UserOutlined />
          </span>
        )}

        {!disabled && (
          <Upload
            accept="image/*"
            showUploadList={false}
            beforeUpload={(file) => pick(file as File)}
            disabled={uploading}
          >
            <button type="button" className="avatar-up__mask" aria-label="上传头像">
              {uploading ? <LoadingOutlined /> : <CameraOutlined />}
              <span>{uploading ? '上传中' : value ? '更换' : '上传'}</span>
            </button>
          </Upload>
        )}

        {value && !disabled && (
          <button
            type="button"
            className="avatar-up__clear"
            aria-label="移除头像"
            onClick={() => onChange?.(undefined)}
          >
            <DeleteOutlined />
          </button>
        )}
      </div>
      {tip && <div className="avatar-up__tip">{tip}</div>}
    </div>
  );
}
