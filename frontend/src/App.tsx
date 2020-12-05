import React, { useState, useEffect, useCallback } from 'react';
import './App.css';
import YtEmbed from './YtEmbed';
import YouTube from 'react-youtube';
import Queue from './Queue';
import QueueItem from './QueueItem';
import Error from './Error';
import Input from './Input';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons';

const youtubeUrlRegex = new RegExp(
  'https://(?:www)?\\.youtu(?:\\.be|be\\.com)/watch\\?v=([^&]+)(?:.*)?',
);

const server =
  process.env.NODE_ENV !== 'development'
    ? 'wss://yt.randomerror.de/api/room'
    : 'ws://localhost:4567/room';
const ws = new WebSocket(server);
ws.onopen = () => {
  const tail = window.location.href.substr(window.location.origin.length + 1);
  const path = window.location.pathname;

  if (youtubeUrlRegex.test(tail)) {
    ws.send('create');
    ws.send(`queue add ${tail}`);
  } else if (path === '' || path === '/') {
    ws.send('create');
  } else {
    const roomId = path.substring(1);
    ws.send(`join ${roomId}`);
  }
};

function App() {
  const [player, setPlayer] = useState<any | null>(null);
  const [preloadTime, setPreloadTime] = useState<number | null>(null);
  const [nextReadyCheck, setNextReadyCheck] = useState<number>(100);
  const [initialized, setInitialized] = useState<boolean>(false);
  const [oldState, setOldState] = useState<number>(
    YouTube.PlayerState.UNSTARTED,
  );
  const [videoId, setVideoId] = useState<string>('');
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [errors, setErrors] = useState<Error[]>([]);
  const [numUsers, setNumUsers] = useState(1);
  const [hasEverPlayed, setHasEverPlayed] = useState<boolean>(false);
  const [overlay, setOverlay] = useState<'PAUSED' | 'SYNCING' | null>(null);

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
    ws.onerror = () => console.log('error');
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
      } else if (msg === 'play') {
        if (player?.getPlayerState() !== YouTube.PlayerState.PLAYING) {
          player?.playVideo();
        }
        setOverlay(null);
      } else if (msg.startsWith('pause')) {
        const timestamp = parseFloat(msg.split(' ')[1]);
        const shouldSeek = Math.abs(player?.getCurrentTime() - timestamp) > 1;
        setOverlay('PAUSED');
        if (shouldSeek) {
          setTimeout(() => {
            player?.seekTo(timestamp, true);
          }, 150);
        }
        if (
          player?.getPlayerState() === YouTube.PlayerState.PLAYING ||
          player?.getPlayerState() === YouTube.PlayerState.BUFFERING
        ) {
          if (hasEverPlayed) {
            console.log('pause setting pause');
            player?.pauseVideo();
          }
        }
      } else if (msg.startsWith('ready?')) {
        const timestamp = parseFloat(msg.split(' ')[1]);
        setNextReadyCheck(100);
        setPreloadTime(timestamp);
        setOverlay('SYNCING');
      } else if (msg.startsWith('video')) {
        const videoId = msg.split(' ')[1];
        setVideoId(videoId);
        setHasEverPlayed(false);
      } else if (msg.startsWith('queue add')) {
        const msgParts = msg.split(' ');
        const queueItem: QueueItem = JSON.parse(msgParts.slice(2).join(' '));
        setQueue((queue) => [...queue, queueItem]);
      } else if (msg.startsWith('queue rm')) {
        const videoId = msg.split(' ')[2];
        setQueue((queue) => queue.filter((video) => video.id !== videoId));
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
      }
    };
    return () => {
      ws.onmessage = null;
    };
  }, [
    player,
    setPreloadTime,
    setQueue,
    setNumUsers,
    setNextReadyCheck,
    setErrors,
    hasEverPlayed,
    setHasEverPlayed,
    setOverlay,
  ]);

  useEffect(() => {
    if (preloadTime === null) {
      return;
    }

    const readyCheck = setInterval(() => {
      if (preloadTime === null) {
        return;
      }
      console.log(
        `ready? ${player?.getPlayerState()} ${preloadTime} ${player?.getCurrentTime()} ${player?.getVideoLoadedFraction()}`,
      );
      if (
        Math.abs(player?.getCurrentTime() - preloadTime) <= 1 &&
        player?.getPlayerState() !== YouTube.PlayerState.BUFFERING
      ) {
        console.log(`sending ready ${player?.getCurrentTime()}`);
        ws.send(`ready ${player?.getCurrentTime()}`);
        setPreloadTime(null);
      } else {
        if (
          player?.getPlayerState() === YouTube.PlayerState.PLAYING ||
          player?.getPlayerState() === YouTube.PlayerState.BUFFERING
        ) {
          if (hasEverPlayed) {
            console.log('ready setting pause');
            player?.pauseVideo();
          }
        }

        player?.seekTo(preloadTime, true);
        // youtube does not update videoLoadedFraction
        // without updated seek event
        setPreloadTime((time) => time!! + 0.01);
        setNextReadyCheck((check) => Math.min(5000, check * 2));
      }
    }, nextReadyCheck);

    return () => clearInterval(readyCheck);
  }, [
    player,
    preloadTime,
    hasEverPlayed,
    setPreloadTime,
    nextReadyCheck,
    setNextReadyCheck,
  ]);

  const onStateChange = useCallback(() => {
    const memOldState = oldState;
    const newState = player.getPlayerState();
    setOldState(newState);
    console.log('player state changed to ' + newState);
    if (newState === YouTube.PlayerState.PAUSED) {
      if (preloadTime === null) {
        setOverlay('PAUSED');
      }
      if (hasEverPlayed) {
        console.log(`sending pause ${player.getCurrentTime()}`);
        ws.send(`pause ${player.getCurrentTime()}`);
      }
    } else if (newState === YouTube.PlayerState.PLAYING) {
      setHasEverPlayed(true);
      if (initialized) {
        console.log(`sending play ${player.getCurrentTime()}`);
        ws.send(`play ${player.getCurrentTime()}`);
        setOverlay(null);
      } else {
        setInitialized(true);
        // the youtube player behaves strange if it is paused
        // almost immediately after starting, so delay sync
        console.log('sending sync');
        ws.send('sync');
      }
    } else if (newState === YouTube.PlayerState.ENDED) {
      console.log(`sending end`);
      ws.send(`end ${videoId}`);
    } else if (
      newState === YouTube.PlayerState.BUFFERING &&
      memOldState === YouTube.PlayerState.PLAYING
    ) {
      console.log(`sending buffer ${player.getCurrentTime()}`);
      ws.send(`buffer ${player.getCurrentTime()}`);
    }
  }, [
    player,
    videoId,
    oldState,
    setOldState,
    setOverlay,
    hasEverPlayed,
    setHasEverPlayed,
    preloadTime,
    initialized,
    setInitialized,
  ]);

  const onPlaybackRateChange = (event: { target: any; data: number }) => {
    ws.send(`speed ${event.data}`);
  };

  const ready = useCallback((player: any) => {
    setPlayer(player);
  }, []);
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
  return (
    <>
      <main className="with-sidebar">
        <div>
          <section className="video">
            <div className="embed">
              <YtEmbed
                videoId={videoId}
                onStateChange={onStateChange}
                onPlaybackRateChange={onPlaybackRateChange}
                setPlayer={ready}
                overlay={overlay}
              />
            </div>
          </section>
          <section>
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
    </>
  );
}

export default App;
