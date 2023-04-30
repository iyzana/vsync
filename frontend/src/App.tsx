import { useState, useEffect, useCallback, useRef } from 'react';
import './App.css';
import Notification from './model/Notification';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons';
import Player from './component/Player';
import Sidebar from './component/Sidebar';
import { WebsocketContext } from './context/websocket';

const urlRegex = new RegExp('^(ftp|https?)://.*');

const server =
  process.env.NODE_ENV !== 'development'
    ? `wss://${window.location.host}/api/room`
    : 'ws://localhost:4567/api/room';
const ws = new WebSocket(server);
ws.onopen = () => {
  const path = (window.location.pathname + window.location.search).substring(1);

  if (urlRegex.test(path)) {
    ws.send('create');
    ws.send(`queue add ${path}`);
  } else if (path === '') {
    ws.send('create');
  } else {
    ws.send(`join ${path}`);
  }
};

function App() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const messageCallbacks = useRef<Record<string, (msg: string) => void>>({});

  const addNotification = useCallback((notification: Notification) => {
    setNotifications((notifications) => [
      ...notifications.filter((notification) => !notification.permanent),
      notification,
      ...notifications.filter((notification) => notification.permanent),
    ]);
  }, []);

  useEffect(() => {
    ws.onclose = () => {
      console.log('disconnected');
      // wait 200ms before showing connection lost because on site-reload
      // firefox first closes the websocket resulting in the error briefly
      // showing up when it is not necessary
      const timeout = setTimeout(() => {
        addNotification({
          message: 'Connection lost',
          level: 'error',
          permanent: true,
        });
      }, 200);
      return () => clearTimeout(timeout);
    };
  }, [addNotification]);

  useEffect(() => {
    ws.onmessage = (ev: MessageEvent) => {
      const msg = ev.data as string;
      console.log(`received websocket message: ${msg}`);
      if (msg.startsWith('create')) {
        const roomId = msg.split(' ')[1];
        window.history.pushState(roomId, '', `/${roomId}`);
      } else if (msg === 'invalid command') {
        addNotification({
          message: 'You found a bug',
          level: 'error',
          permanent: false,
        });
      } else if (msg === 'server full') {
        addNotification({
          message: 'Server is too full',
          level: 'error',
          permanent: true,
        });
      } else {
        Object.values(messageCallbacks.current).forEach((callback) =>
          callback(msg),
        );
      }
    };
    return () => {
      ws.onmessage = null;
    };
  }, [addNotification]);

  useEffect(() => {
    if (!notifications.find((notification) => !notification.permanent)) {
      // no non-permanent notifications
      return;
    }
    const timeout = setTimeout(() => {
      setNotifications((notifications) => {
        const dismissIndex = notifications.findIndex(
          (notification) => !notification.permanent,
        );
        console.log(`dismissing ${dismissIndex}`);
        return dismissIndex === -1 ? notifications : notifications.slice(1);
      });
    }, 2000);
    return () => clearTimeout(timeout);
  }, [notifications]);

  const addMessageCallback = useCallback(
    (id: string, callback: (msg: string) => void) => {
      messageCallbacks.current[id] = callback;
    },
    [],
  );
  const removeMessageCallback = useCallback((id: string) => {
    delete messageCallbacks.current[id];
  }, []);
  const sendMessage = useCallback((message: string) => {
    console.log('sending websocket message: ' + message);
    ws.send(message);
  }, []);

  useEffect(() => {
    document.onkeydown = (event: KeyboardEvent) => {
      if (document.activeElement instanceof HTMLInputElement) {
        return;
      }
      if (event.key === 'N') {
        sendMessage('skip');
      }
    };
    return () => {
      document.onkeydown = null;
    };
  }, [sendMessage]);

  return (
    <WebsocketContext.Provider
      value={{ addMessageCallback, removeMessageCallback, sendMessage }}
    >
      <div className="container">
        <main className="with-sidebar">
          <section>
            <Player />
          </section>
          <section className="aside">
            <Sidebar
              notifications={notifications}
              addNotification={addNotification}
            />
          </section>
        </main>
        <footer className="footer">
          <a
            className="social"
            href="https://github.com/iyzana/yt-sync"
            target="_blank"
            rel="noreferrer"
            aria-label="GitHub project"
          >
            <FontAwesomeIcon icon={faGithub} size="lg" />
          </a>
        </footer>
      </div>
    </WebsocketContext.Provider>
  );
}

export default App;
