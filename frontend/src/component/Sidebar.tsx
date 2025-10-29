import "./Sidebar.css";
import Input from "./Input";
import Queue from "./Queue";
import Notification from "../model/Notification";
import Infobox from "./Infobox";
import { useCallback, useContext } from "react";
import { WebsocketContext } from "../context/websocket";
import VideoInfo from "./VideoInfo";

interface SidebarProps {
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
}

function Sidebar({ notifications, addNotification }: SidebarProps) {
  const { sendMessage } = useContext(WebsocketContext);

  const addToQueue = useCallback(
    (input: string) => {
      sendMessage(`queue add ${input}`);
    },
    [sendMessage],
  );

  return (
    <div className="sidebar">
      <Infobox
        notifications={notifications}
        addNotification={addNotification}
      />
      <VideoInfo />
      <Queue addNotification={addNotification} />
      <Input addToQueue={addToQueue} />
    </div>
  );
}

export default Sidebar;
