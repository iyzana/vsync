import './Queue.css';
import QueueItem from '../model/QueueItem';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTimes, faAngleDoubleRight } from '@fortawesome/free-solid-svg-icons';
import { ReactSortable } from 'react-sortablejs';
import { useCallback, useContext, useState } from 'react';
import Notification from '../model/Notification';
import { useWebsocketMessages } from '../hook/websocket-messages';
import { WebsocketContext } from '../context/websocket';
import FavIcon from './FavIcon';
import Thumbnail from './Thumbnail';
import Host from './Host';
import VideoMetadataLine from './VideoMetadataLine';

const getDomain: (item: QueueItem) => string | null = (item: QueueItem) => {
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

interface QueueProps {
  addNotification: (notification: Notification) => void;
}

function Queue({ addNotification }: QueueProps) {
  const [queue, setQueue] = useState<QueueItem[]>([]);

  useWebsocketMessages(
    (msg: string) => {
      if (msg.startsWith('queue add')) {
        const msgParts = msg.split(' ');
        const queueItem: QueueItem = JSON.parse(msgParts.slice(2).join(' '));
        setQueue((queue) => {
          const index = queue.findIndex((video) => video.id === queueItem.id);
          console.log({ index });
          if (index === -1) {
            let newQueue = [...queue, queueItem];
            newQueue.sort((a, b) => +a.loading - +b.loading);
            return newQueue;
          } else {
            if (queue[index].loading && !queueItem.loading) {
              let newQueue = [...queue];
              newQueue.splice(index, 1);
              newQueue.push(queueItem);
              newQueue.sort((a, b) => +a.loading - +b.loading);
              return newQueue;
            } else {
              let newQueue = [...queue];
              newQueue.splice(index, 1, queueItem);
              return newQueue;
            }
          }
        });
      } else if (msg.startsWith('queue rm')) {
        const id = msg.split(' ')[2];
        setQueue((queue) => queue.filter((video) => video.id !== id));
      } else if (msg.startsWith('queue order')) {
        const order = msg.split(' ')[2].split(',');
        setQueue((queue) => {
          const sortedQueue = [...queue];
          sortedQueue.sort((a, b) => {
            if (a.loading !== b.loading) {
              return +a.loading - +b.loading;
            }
            return order.indexOf(a.id) - order.indexOf(b.id);
          });
          return sortedQueue;
        });
      } else if (msg === 'queue err not-found') {
        addNotification({
          message: 'No video found',
          level: 'info',
          permanent: false,
        });
      } else if (msg === 'queue err duplicate') {
        addNotification({
          message: 'Already in queue',
          level: 'info',
          permanent: false,
        });
      }
    },
    [addNotification],
  );

  const { sendMessage } = useContext(WebsocketContext);
  const skip = () => sendMessage('skip');
  const reorderQueue = useCallback(
    (videos: QueueItem[]) => {
      const oldOrder = queue
        .filter((video) => !video.loading)
        .map((video) => video.id);
      const newOrder = videos
        .filter((video) => !video.loading)
        .map((video) => video.id);
      if (
        oldOrder.length !== newOrder.length ||
        [...oldOrder].sort().join() !== [...newOrder].sort().join() ||
        oldOrder.join() === newOrder.join()
      ) {
        return;
      }

      sendMessage(`queue order ${newOrder.join(',')}`);
      setQueue(videos);
    },
    [queue, sendMessage],
  );

  const removeVideo = (id: string) => sendMessage(`queue rm ${id}`);

  return (
    <>
      <div className="queue">
        <div className="queue-header">
          <h3 className="title">Queue</h3>
          <div className="status">
            {queue.filter(video => !video.loading).length === 0 ? null : (
              <button className="skip" onClick={skip}>
                Play next <FontAwesomeIcon icon={faAngleDoubleRight} />
              </button>
            )}
          </div>
        </div>
        <ReactSortable
          list={queue}
          setList={reorderQueue}
          className="queue-list"
        >
          {queue.map((item) => {
            return (
              <li key={item.id} className="queue-item">
                <div className="queue-video-info">
                  <Thumbnail
                    thumbnailUrl={item.thumbnail || null}
                    loading={item.loading}
                  />
                  <div>
                    <div className='queue-video-title'>
                      {item.metadata?.title || (item.loading ? 'Loading...' : 'No title')}
                    </div>
                    <VideoMetadataLine metadata={item.metadata} />
                    <Host element={item} />
                  </div>
                  {!item.loading ? (
                    <button
                      className="remove"
                      onClick={() => removeVideo(item.id)}
                    >
                      <FontAwesomeIcon icon={faTimes} />
                    </button>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ReactSortable>
      </div>
    </>
  );
}

export default Queue;
