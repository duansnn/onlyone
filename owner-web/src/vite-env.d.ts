/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string;
  readonly VITE_API_BASE_URL: string;
  readonly VITE_BACKEND_ORIGIN: string;
  readonly VITE_DEV_PORT: string;
  readonly VITE_WS_BASE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
