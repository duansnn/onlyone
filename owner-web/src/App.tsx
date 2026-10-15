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
          colorPrimary: '#EC6541',
          colorPrimaryHover: '#D95334',
          colorPrimaryActive: '#C44828',
          colorLink: '#D95334',
          colorInfo: '#F9815E',
          colorSuccess: '#7BA05B',
          colorWarning: '#F5A623',
          colorError: '#D64545',
          colorBgLayout: '#FFFFFF',
          colorBgContainer: '#FFFFFF',
          colorBgElevated: '#FFFFFF',
          colorBorder: '#E8E8E8',
          colorBorderSecondary: '#F0F0F0',
          colorSplit: '#EFEFEF',
          colorFillAlter: '#FAFAFA',
          colorFillSecondary: '#F5F5F5',
          colorText: '#131313',
          colorTextSecondary: '#4A443F',
          colorTextTertiary: '#8A8178',
          colorTextLightSolid: '#FFFFFF',
          borderRadius: 14,
          borderRadiusLG: 22,
          fontSize: 14,
          controlOutline: 'rgba(236, 101, 65, 0.16)',
          boxShadow: '0 14px 36px rgba(70, 50, 40, 0.07)',
          boxShadowSecondary: '0 8px 20px rgba(70, 50, 40, 0.05)',
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
            activeBorderColor: '#EC6541',
            hoverBorderColor: '#D9B49E',
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
