import './Queue.css';
import QueueItem from '../model/QueueItem';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTimes, faAngleDoubleRight } from '@fortawesome/free-solid-svg-icons';
import { ReactSortable } from 'react-sortablejs';
import { useCallback, useEffect, useState } from 'react';
import Notification from '../model/Notification';

interface QueueProps {
  addMessageCallback: (
    name: string,
    callback: (message: string) => void,
  ) => void;
  removeMessageCallback: (name: string) => void;
  sendMessage: (message: string) => void;
  addNotification: (notification: Notification) => void;
  setWorking: (working: boolean) => void;
}

function Queue({
  addMessageCallback,
  removeMessageCallback,
  sendMessage,
  addNotification,
  setWorking,
}: QueueProps) {
  const [queue, setQueue] = useState<QueueItem[]>([]);

  const messageCallback = useCallback(
    (msg: string) => {
      if (msg.startsWith('video')) {
        setWorking(false);
      } else if (msg.startsWith('queue add')) {
        const msgParts = msg.split(' ');
        const queueItem: QueueItem = JSON.parse(msgParts.slice(2).join(' '));
        setQueue((queue) => [...queue, queueItem]);
        setWorking(false);
      } else if (msg.startsWith('queue rm')) {
        const id = msg.split(' ')[2];
        setQueue((queue) => queue.filter((video) => video.id !== id));
      } else if (msg.startsWith('queue order')) {
        const order = msg.split(' ')[2].split(',');
        setQueue((queue) => {
          const sortedQueue = [...queue];
          sortedQueue.sort((a, b) => {
            return order.indexOf(a.id) - order.indexOf(b.id);
          });
          return sortedQueue;
        });
      } else if (msg === 'queue err not-found') {
        addNotification({
          message: 'No video found',
          level: 'error',
          permanent: false,
        });
        setWorking(false);
      } else if (msg === 'queue err duplicate') {
        addNotification({
          message: 'Already in queue',
          level: 'error',
          permanent: false,
        });
        setWorking(false);
      }
    },
    [setQueue, addNotification, setWorking],
  );

  useEffect(() => {
    addMessageCallback('queue', messageCallback);
    return () => removeMessageCallback('queue');
  }, [messageCallback, addMessageCallback, removeMessageCallback]);

  const skip = () => sendMessage('skip');
  const reorderQueue = useCallback(
    (videos: QueueItem[]) => {
      const oldOrder = queue.map((video) => video.id);
      const newOrder = videos.map((video) => video.id);
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
    [queue, setQueue, sendMessage],
  );

  const removeVideo = (id: string) => sendMessage(`queue rm ${id}`);

  const faviconUrl = useCallback((url: string, originalQuery: string) => {
    let baseUrl;
    try {
      baseUrl = new URL(originalQuery);
    } catch (e) {
      baseUrl = new URL(url);
    }
    baseUrl.search = '';
    baseUrl.pathname = 'favicon.ico';
    return baseUrl;
  }, []);

  return (
    <>
      <div className="queue">
        <div className="queue-header">
          <h3 className="title">Queue</h3>
          <div className="status">
            {queue.length === 0 ? null : (
              <button className="skip" onClick={skip}>
                SKIP <FontAwesomeIcon icon={faAngleDoubleRight} />
              </button>
            )}
          </div>
        </div>
        <ReactSortable
          list={queue}
          setList={reorderQueue}
          className="queue-list"
        >
          {queue.map(({ title, thumbnail, id, url, originalQuery }) => {
            const favicon = faviconUrl(url, originalQuery);
            return (
              <li key={id} className="queue-item">
                <div className="video-info">
                  <img className="thumbnail" src={thumbnail} alt="" />
                  <div>
                    <img
                      className="favicon"
                      src={favicon.toString()}
                      alt={`favicon of ${favicon.host}`}
                    ></img>{' '}
                    {title}
                  </div>
                </div>
                <button className="remove" onClick={() => removeVideo(id)}>
                  <FontAwesomeIcon icon={faTimes} />
                </button>
              </li>
            );
          })}
        </ReactSortable>
      </div>
    </>
  );
}

export default Queue;
