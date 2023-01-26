import './Sidebar.css';
import Input from './Input';
import Queue from './Queue';
import Notification from '../model/Notification';
import Infobox from './Infobox';
import { useCallback, useState } from 'react';

interface SidebarProps {
  addMessageCallback: (
    name: string,
    callback: (message: string) => void,
  ) => void;
  removeMessageCallback: (name: string) => void;
  sendMessage: (message: string) => void;
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
}

function Sidebar({
  addMessageCallback,
  removeMessageCallback,
  sendMessage,
  notifications,
  addNotification,
}: SidebarProps) {
  const [working, setWorking] = useState(false);

  const addToQueue = useCallback(
    (input: string) => {
      sendMessage(`queue add ${input}`);
      setWorking(true);
    },
    [sendMessage, setWorking],
  );

  return (
    <div className="sidebar">
      <Infobox
        addMessageCallback={addMessageCallback}
        removeMessageCallback={removeMessageCallback}
        sendMessage={sendMessage}
        notifications={notifications}
        addNotification={addNotification}
      />
      <Queue
        addMessageCallback={addMessageCallback}
        removeMessageCallback={removeMessageCallback}
        sendMessage={sendMessage}
        addNotification={addNotification}
        setWorking={setWorking}
      />
      <Input addToQueue={addToQueue} working={working} />
    </div>
  );
}

export default Sidebar;
