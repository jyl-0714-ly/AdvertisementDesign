/// <reference types="vite/client" />

declare module 'node:url' {
  export function fileURLToPath(url: any): string
  export const URL: {
    new (input: any, base?: any): any
  }
}

interface ImportMeta {
  readonly url: string
}
