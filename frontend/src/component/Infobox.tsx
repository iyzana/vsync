import './Infobox.css';
import { useCallback, useState } from 'react';
import Notification from '../model/Notification';
import { useWebsocketMessages } from '../hook/websocket-messages';

interface InfoboxProps {
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
}

function Infobox({ notifications, addNotification }: InfoboxProps) {
  const [numUsers, setNumUsers] = useState(1);

  useWebsocketMessages(
    'infobox',
    useCallback((msg: string) => {
      if (msg.startsWith('users')) {
        const users = parseInt(msg.split(' ')[1]);
        setNumUsers(users);
      }
    }, []),
  );

  const copyLink = () => {
    if (!navigator.clipboard) {
      addNotification({
        message: 'Not supported',
        level: 'error',
        permanent: false,
      });
      return;
    }
    navigator.clipboard.writeText(window.location.href);
    addNotification({
      message: 'Room link copied to clipboard',
      level: 'success',
      permanent: false,
    });
  };

  const notification = notifications[0];

  return (
    <div
      className={`infobox ${notification != null ? notification.level : ''}`}
    >
      {notification != null ? (
        <>
          <span>{notifications[0].message}</span>
          {notifications.length > 1 ? (
            <span className="remaining">
              {notifications.length - 1} remaining
            </span>
          ) : null}
        </>
      ) : (
        <>
          <span className="connections">
            {numUsers === 1 ? 'No one else connected' : `${numUsers} connected`}
          </span>
          <div>
            <button className="copylink" onClick={copyLink}>
              Copy room link
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default Infobox;
