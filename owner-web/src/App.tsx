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
          colorPrimary: '#1C1C1E',
          colorPrimaryHover: '#2A2A2C',
          colorPrimaryActive: '#000000',
          colorLink: '#1C1C1E',
          colorInfo: '#6E62B0',
          colorSuccess: '#6B8F4E',
          colorWarning: '#C9A227',
          colorError: '#C45C5C',
          colorBgLayout: '#D4CDC0',
          colorBgContainer: '#FFFFFF',
          colorBgElevated: '#FFFFFF',
          colorBorder: '#D2C8B8',
          colorBorderSecondary: '#E3DACB',
          colorSplit: '#DDD3C4',
          colorFillAlter: '#EBE4D6',
          colorFillSecondary: '#E4DCCE',
          colorText: '#121212',
          colorTextSecondary: '#3A3632',
          colorTextTertiary: '#4A453F',
          colorTextLightSolid: '#FFFFFF',
          borderRadius: 14,
          borderRadiusLG: 22,
          fontSize: 14,
          controlOutline: 'rgba(28, 28, 30, 0.12)',
          boxShadow: '0 14px 36px rgba(40, 36, 28, 0.06)',
          boxShadowSecondary: '0 8px 20px rgba(40, 36, 28, 0.05)',
        },
        components: {
          Button: {
            primaryShadow: 'none',
            borderRadius: 14,
          },
          Card: {
            borderRadiusLG: 24,
          },
          Input: {
            activeBorderColor: '#1C1C1E',
            hoverBorderColor: '#8A867C',
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
