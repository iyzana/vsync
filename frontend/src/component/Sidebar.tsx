import './Sidebar.css';
import Input from './Input';
import Queue from './Queue';
import Notification from '../model/Notification';
import Infobox from './Infobox';
import { useCallback, useContext, useState } from 'react';
import { WebsocketContext } from '../context/websocket';

interface SidebarProps {
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
}

function Sidebar({ notifications, addNotification }: SidebarProps) {
  const [working, setWorking] = useState(false);
  const { sendMessage } = useContext(WebsocketContext);

  const addToQueue = useCallback(
    (input: string) => {
      sendMessage(`queue add ${input}`);
      setWorking(true);
    },
    [sendMessage],
  );

  return (
    <div className="sidebar">
      <Infobox
        notifications={notifications}
        addNotification={addNotification}
      />
      <Queue addNotification={addNotification} setWorking={setWorking} />
      <Input addToQueue={addToQueue} working={working} />
    </div>
  );
}

export default Sidebar;
