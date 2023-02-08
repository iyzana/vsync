import { useContext, useEffect } from 'react';
import { WebsocketContext } from '../context/websocket';

export function useWebsocketMessages(
  componentName: string,
  messageCallback: (msg: string) => void,
) {
  const { addMessageCallback, removeMessageCallback } =
    useContext(WebsocketContext);
  useEffect(() => {
    addMessageCallback(componentName, messageCallback);
    return () => removeMessageCallback(componentName);
  }, [
    componentName,
    messageCallback,
    addMessageCallback,
    removeMessageCallback,
  ]);
}
