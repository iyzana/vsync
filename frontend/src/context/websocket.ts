import { createContext } from 'react';

export const WebsocketContext = createContext<{
  addMessageCallback: (
    name: string,
    callback: (message: string) => void,
  ) => void;
  removeMessageCallback: (name: string) => void;
  sendMessage: (message: string) => void;
}>(null as any);
