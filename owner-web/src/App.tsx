import { useEffect } from 'react';
import { App as AntdApp, ConfigProvider, theme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { RouterProvider } from 'react-router-dom';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import router from '@/router';
import { useToastStore } from '@/utils/feedback';

dayjs.locale('zh-cn');

/**
 * 把 antd App 上下文中的 message 实例注册到全局 toast，
 * 这样 axios 拦截器等非组件模块也能正确弹出提示（含主题与国际化）。
 */
function ToastBridge() {
  const { message } = AntdApp.useApp();
  const setApi = useToastStore((state) => state.setApi);

  useEffect(() => {
    setApi(message);
    return () => setApi(null);
  }, [message, setApi]);

  return null;
}

export default function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      // antd 默认会在「两个汉字」的按钮文案中间插入一个空格（如 保 存 / 取 消），
      // 关闭后文案与代码保持一致，便于阅读与自动化测试定位。
      button={{ autoInsertSpace: false }}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#111111',
          colorPrimaryHover: '#000000',
          colorPrimaryActive: '#333333',
          colorLink: '#2563EB',
          colorInfo: '#2563EB',
          colorSuccess: '#16A34A',
          colorWarning: '#EAB308',
          colorError: '#DC2626',
          colorBgLayout: '#FFFFFF',
          colorBgContainer: '#FFFFFF',
          colorBgElevated: '#FFFFFF',
          colorBorder: '#E5E5E5',
          colorBorderSecondary: '#EEEEEE',
          colorSplit: '#EEEEEE',
          colorFillAlter: '#F5F5F5',
          colorFillSecondary: '#F5F5F5',
          colorText: '#111111',
          colorTextSecondary: '#525252',
          colorTextTertiary: '#737373',
          colorTextLightSolid: '#FFFFFF',
          borderRadius: 12,
          borderRadiusLG: 18,
          fontSize: 14,
          fontFamily: "'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif",
          controlOutline: 'rgba(17, 17, 17, 0.12)',
          boxShadow: 'none',
          boxShadowSecondary: 'none',
        },
        components: {
          Button: {
            primaryShadow: 'none',
            borderRadius: 999,
          },
          Card: {
            borderRadiusLG: 20,
          },
          Input: {
            activeBorderColor: '#111111',
            hoverBorderColor: '#111111',
          },
        },
      }}
    >
      <AntdApp>
        <ToastBridge />
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  );
}
