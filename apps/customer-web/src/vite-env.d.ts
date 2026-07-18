/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_CUSTOMER_BFF_BASE_URL?: string;
  readonly VITE_CUSTOMER_LOGIN_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
