import { DependencyList, useCallback, useContext, useEffect } from 'react';
import { WebsocketContext } from '../context/websocket';

export function useWebsocketMessages(
  handler: (msg: string) => void,
  deps: DependencyList,
) {
  const id = handler.toString();
  const { addMessageCallback, removeMessageCallback } =
    useContext(WebsocketContext);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const callback = useCallback(handler, deps);

  useEffect(() => {
    addMessageCallback(id, callback);
    return () => removeMessageCallback(id);
  }, [id, callback, addMessageCallback, removeMessageCallback]);
}

export function useWebsocketClose(handler: () => void, deps: DependencyList) {
  const id = handler.toString();
  const { addOnCloseCallback, removeOnCloseCallback } =
    useContext(WebsocketContext);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const callback = useCallback(handler, deps);

  useEffect(() => {
    addOnCloseCallback(id, callback);
    return () => removeOnCloseCallback(id);
  }, [id, callback, addOnCloseCallback, removeOnCloseCallback]);
}
