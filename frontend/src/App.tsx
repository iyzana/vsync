import { useState, useEffect, useCallback } from 'react';
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
    : 'ws://localhost:4567/room';
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
  const [messageCallbacks, setMessageCallbacks] = useState<{
    [key: string]: (msg: string) => void;
  }>({});

  const addNotification = useCallback((notification: Notification) => {
    setNotifications((notifications) => [...notifications, notification]);
  }, []);

  useEffect(() => {
    ws.onclose = () => {
      console.log('disconnected');
      addNotification({
        message: 'Connection lost',
        level: 'error',
        permanent: true,
      });
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
        Object.values(messageCallbacks).forEach((callback) => callback(msg));
      }
    };
    return () => {
      ws.onmessage = null;
    };
  }, [addNotification, messageCallbacks]);

  useEffect(() => {
    if (notifications.length === 0) {
      return;
    }
    const timeout = setTimeout(() => {
      setNotifications((notifications) => {
        const dismissIndex = notifications.findIndex(
          (notification) => !notification.permanent,
        );
        return notifications.splice(dismissIndex, 1);
      });
    }, 3000);
    return () => clearTimeout(timeout);
  }, [notifications]);

  const addMessageCallback = useCallback(
    (name: string, callback: (msg: string) => void) => {
      console.log(`adding callback ${name}`);
      setMessageCallbacks((callbacks) =>
        Object.assign(callbacks, { [name]: callback }),
      );
    },
    [],
  );
  const removeMessageCallback = useCallback((name: string) => {
    console.log(`removing callback ${name}`);
    setMessageCallbacks((callbacks) => {
      const { [name]: removed, ...remaining } = callbacks;
      return remaining;
    });
  }, []);
  const sendMessage = useCallback((message: string) => {
    console.log('sending websocket message: ' + message);
    ws.send(message);
  }, []);

  return (
    <WebsocketContext.Provider
      value={{ addMessageCallback, removeMessageCallback, sendMessage }}
    >
      <div className="container">
        <main className="with-sidebar">
          <div>
            <section className="video">
              <div className="embed">
                <Player />
              </div>
            </section>
            <section className="aside">
              <Sidebar
                notifications={notifications}
                addNotification={addNotification}
              />
            </section>
          </div>
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
