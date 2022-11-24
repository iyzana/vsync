import { useState, useEffect, useCallback } from 'react';
import './App.css';
import Queue from './Queue';
import QueueItem from './QueueItem';
import Error from './Error';
import Input from './Input';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons';
import Player from './Player';

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
  const [msg, setMsg] = useState<string | null>(null);
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [errors, setErrors] = useState<Error[]>([]);
  const [numUsers, setNumUsers] = useState(1);

  useEffect(() => {
    ws.onclose = () => {
      console.log('disconnected');
      setTimeout(() => {
        setErrors((errors) => [
          ...errors,
          { message: 'Connection lost', permanent: true },
        ]);
      }, 200);
    };
  }, [setErrors]);

  useEffect(() => {
    ws.onmessage = (ev: MessageEvent) => {
      const msg = ev.data as string;
      console.log(`received ${msg}`);
      if (msg.startsWith('create')) {
        const roomId = msg.split(' ')[1];
        window.history.pushState(roomId, '', `/${roomId}`);
      } else if (msg === 'invalid command') {
        setErrors((errors) => [
          ...errors,
          { message: 'You found a bug', permanent: false },
        ]);
      } else if (msg === 'server full') {
        setErrors((errors) => [
          ...errors,
          { message: 'Server is too full', permanent: false },
        ]);
      } else if (msg.startsWith('queue add')) {
        const msgParts = msg.split(' ');
        const queueItem: QueueItem = JSON.parse(msgParts.slice(2).join(' '));
        setQueue((queue) => [...queue, queueItem]);
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
        setErrors((errors) => [
          ...errors,
          { message: 'Video not found', permanent: false },
        ]);
      } else if (msg === 'queue err duplicate') {
        setErrors((errors) => [
          ...errors,
          { message: 'Already in queue', permanent: false },
        ]);
      } else if (msg.startsWith('users')) {
        const users = parseInt(msg.split(' ')[1]);
        setNumUsers(users);
      } else {
        setMsg(msg);
      }
    };
    return () => {
      ws.onmessage = null;
    };
  }, [setQueue, setNumUsers, setErrors, setMsg]);

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

      ws.send(`queue order ${newOrder.join(',')}`);
      setQueue(videos);
    },
    [queue, setQueue],
  );
  const sendMessage = useCallback((message: string) => ws.send(message), []);
  return (
    <div className="container">
      <main className="with-sidebar">
        <div>
          <section className="video">
            <div className="embed">
              <Player msg={msg} sendMessage={sendMessage} />
            </div>
          </section>
          <section className="aside">
            <div className="control">
              <Queue
                videos={queue}
                setVideos={reorderQueue}
                removeVideo={(video) => ws.send(`queue rm ${video}`)}
                skip={() => ws.send('skip')}
                numUsers={numUsers}
              />
              <Input ws={ws} errors={errors} setErrors={setErrors} />
            </div>
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
  );
}

export default App;
