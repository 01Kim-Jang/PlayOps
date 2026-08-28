/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_PLAYOPS_ENV_FILE_PRESENT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
