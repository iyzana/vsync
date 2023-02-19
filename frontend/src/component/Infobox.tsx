import './Infobox.css';
import { useState } from 'react';
import Notification from '../model/Notification';
import { useWebsocketMessages } from '../hook/websocket-messages';
import Dialog from './Dialog';
import About from './About';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faQuestionCircle } from '@fortawesome/free-solid-svg-icons';

interface InfoboxProps {
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
}

function Infobox({ notifications, addNotification }: InfoboxProps) {
  const [about, setAbout] = useState(false);
  const [numUsers, setNumUsers] = useState(1);

  useWebsocketMessages((msg: string) => {
    if (msg.startsWith('users')) {
      const users = parseInt(msg.split(' ')[1]);
      setNumUsers(users);
    }
  }, []);

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
          <span className="notification">{notifications[0].message}</span>
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
          <button className="about" onClick={() => setAbout(true)}>
            <FontAwesomeIcon icon={faQuestionCircle} />
          </button>
          <button className="copylink" onClick={copyLink}>
            Copy room link
          </button>
        </>
      )}
      <Dialog open={about} onDismiss={() => setAbout(false)}>
        <About></About>
      </Dialog>
    </div>
  );
}

export default Infobox;
