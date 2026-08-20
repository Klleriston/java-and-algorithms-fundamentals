/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Set at build time for the GitHub Pages bundle, which has no backend to call. */
  readonly VITE_STATIC?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
