import { createContext } from 'react';

export const WebsocketContext = createContext<{
  sendMessage: (message: string) => void;
  addMessageCallback: (
    name: string,
    callback: (message: string) => void,
  ) => void;
  removeMessageCallback: (name: string) => void;
  addOnCloseCallback: (
    name: string,
    callback: () => void,
  ) => void;
  removeOnCloseCallback: (name: string) => void;
}>(null as any);
