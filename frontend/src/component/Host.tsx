import './Host.css';
import QueueItem from "../model/QueueItem";
import { VideoCommand } from "../model/VideoSource";
import FavIcon from "./FavIcon";

const getDomain: (item: QueueItem | VideoCommand) => string | null = (item: QueueItem | VideoCommand) => {
  let baseUrl;
  try {
    baseUrl = new URL(item.originalQuery);
  } catch (e) {
    if (item.source) {
      baseUrl = new URL(item.source?.url);
    } else if (item.favicon) {
      baseUrl = new URL(item.favicon);
    } else {
      return null;
    }
  }
  const host = baseUrl.hostname;
  return host.replace(/^www./, '');
};

interface HostProps {
  element: QueueItem | VideoCommand;
}

function Host({ element }: HostProps) {
  return (<div className="hostname">
    <FavIcon favicon={element.favicon} />{' '}
    <span>{getDomain(element) || ''}</span>
  </div>);
}

export default Host;
