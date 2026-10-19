import { useState } from 'react';
import { Button, Input, Space, Upload } from 'antd';
import { DeleteOutlined, LoadingOutlined, PlusOutlined, StarOutlined } from '@ant-design/icons';
import { uploadApi } from '@/api/mall';
import { toast } from '@/utils/feedback';
import SafeImage from './SafeImage';

interface ImageUploaderProps {
  /** 由 Form.Item 注入 */
  value?: string[];
  /** 由 Form.Item 注入 */
  onChange?: (value: string[]) => void;
  /** 最多几张，默认 8 */
  max?: number;
  /** 后端业务目录，决定落盘位置 */
  folder?: 'product' | 'shop' | 'review' | 'avatar';
  /** 底部提示文案 */
  tip?: string;
}

const MAX_SIZE = 5 * 1024 * 1024;

/**
 * 图片列表编辑器：上传、删除、设主图，也支持直接粘贴外链。
 *
 * 第一张是主图（列表页与订单里用的就是它），所以「设主图」实际是把该图移到数组首位。
 * 上传由后端 {@code POST /api/upload/image} 落到本地磁盘，返回 /files/... 的相对地址。
 */
export default function ImageUploader({
  value = [],
  onChange,
  max = 8,
  folder = 'product',
  tip = '第一张为主图，建议 1:1，单张不超过 5 MB',
}: ImageUploaderProps) {
  const [uploading, setUploading] = useState(false);
  const [manualUrl, setManualUrl] = useState('');

  const images = (value ?? []).filter((item) => item && item.trim());

  const commit = (next: string[]) => onChange?.(next);

  const beforeUpload = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      toast.error('请选择图片文件');
      return false;
    }
    if (file.size > MAX_SIZE) {
      toast.error('图片不能超过 5 MB');
      return false;
    }
    if (images.length >= max) {
      toast.error(`最多 ${max} 张图片`);
      return false;
    }
    setUploading(true);
    try {
      const url = await uploadApi.image(file, folder);
      commit([...images, url]);
      toast.success('图片已上传');
    } catch {
      /* 拦截器已提示 */
    } finally {
      setUploading(false);
    }
    // 阻止 antd 自己发请求
    return false;
  };

  const addManualUrl = () => {
    const url = manualUrl.trim();
    if (!url) return;
    if (!/^(https?:\/\/|\/)/i.test(url)) {
      toast.error('请填写 http(s) 开头的地址，或 /files/ 开头的站内地址');
      return;
    }
    if (images.length >= max) {
      toast.error(`最多 ${max} 张图片`);
      return;
    }
    commit([...images, url]);
    setManualUrl('');
  };

  return (
    <div className="mall-uploader">
      <div className="mall-uploader__grid">
        {images.map((url, index) => (
          <div className="mall-uploader__item" key={`${url}-${index}`}>
            <SafeImage src={url} alt="" className="mall-uploader__img" />
            {index === 0 && <span className="mall-uploader__badge">主图</span>}
            <div className="mall-uploader__ops">
              {index > 0 && (
                <Button
                  size="small"
                  type="text"
                  icon={<StarOutlined />}
                  title="设为主图"
                  onClick={() => {
                    const next = [...images];
                    const [picked] = next.splice(index, 1);
                    commit([picked, ...next]);
                  }}
                />
              )}
              <Button
                size="small"
                type="text"
                danger
                icon={<DeleteOutlined />}
                title="删除"
                onClick={() => commit(images.filter((_, i) => i !== index))}
              />
            </div>
          </div>
        ))}

        {images.length < max && (
          <Upload
            accept="image/png,image/jpeg,image/webp,image/gif"
            showUploadList={false}
            multiple
            beforeUpload={beforeUpload}
          >
            <div className="mall-uploader__add">
              {uploading ? <LoadingOutlined /> : <PlusOutlined />}
              <span>{uploading ? '上传中' : '上传图片'}</span>
            </div>
          </Upload>
        )}
      </div>

      <Space.Compact style={{ width: '100%', marginTop: 8 }}>
        <Input
          value={manualUrl}
          placeholder="或粘贴图片地址（https://... 或 /files/...）"
          onChange={(e) => setManualUrl(e.target.value)}
          onPressEnter={addManualUrl}
        />
        <Button onClick={addManualUrl}>添加地址</Button>
      </Space.Compact>

      <div className="mall-uploader__tip">{tip}</div>
    </div>
  );
}

type SingleProps = Omit<ImageUploaderProps, 'value' | 'onChange' | 'max'> & {
  value?: string;
  onChange?: (value?: string) => void;
};

/** 单图字段（店铺横幅、头像等）：对 Form.Item 暴露 string，内部复用列表逻辑 */
export function SingleImageUploader({ value, onChange, ...rest }: SingleProps) {
  return (
    <ImageUploader
      {...rest}
      max={1}
      value={value ? [value] : []}
      onChange={(list) => onChange?.(list[0])}
    />
  );
}
