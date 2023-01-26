import './Player.css';
import { faPause, faSyncAlt } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useCallback, useEffect, useState } from 'react';
import YoutubePlayer from './YoutubePlayer';
import VideoJsPlayer from './VideoJsPlayer';

function getOverlay(overlay: 'PAUSED' | 'SYNCING' | null) {
  switch (overlay) {
    case 'PAUSED':
      return (
        <div className="aspect-ratio-inner overlay">
          <FontAwesomeIcon icon={faPause} />
          <div>PAUSED</div>
        </div>
      );
    case 'SYNCING':
      return (
        <div className="aspect-ratio-inner overlay">
          <FontAwesomeIcon icon={faSyncAlt} spin />
          <div>SYNCING</div>
        </div>
      );
    default:
      return null;
  }
}

interface PlayerProps {
  addMessageCallback: (
    name: string,
    callback: (message: string) => void,
  ) => void;
  removeMessageCallback: (name: string) => void;
  sendMessage: (message: string) => void;
}

export interface EmbeddedPlayerProps {
  addMessageCallback: (
    name: string,
    callback: (message: string) => void,
  ) => void;
  removeMessageCallback: (name: string) => void;
  videoUrl: string;
  sendMessage: (msg: string) => void;
  setOverlay: (state: 'PAUSED' | 'SYNCING' | null) => void;
  volume: number | null;
  setVolume: (volume: number) => void;
  initialized: boolean;
  setInitialized: (initialized: boolean) => void;
}

function isYoutubeUrl(url: string): boolean {
  return (
    url.startsWith('https://www.youtube.com/') ||
    url.startsWith('https://youtu.be/')
  );
}

function Player({
  addMessageCallback,
  removeMessageCallback,
  sendMessage,
}: PlayerProps) {
  const [overlay, setOverlay] = useState<'PAUSED' | 'SYNCING' | null>(null);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [volume, setVolume] = useState<number | null>(null);
  const [initialized, setInitialized] = useState<boolean>(false);

  const messageCallback = useCallback(
    (msg: string) => {
      if (msg === 'play') {
        setOverlay(null);
      } else if (msg.startsWith('pause')) {
        setOverlay('PAUSED');
      } else if (msg.startsWith('ready?')) {
        setOverlay('SYNCING');
      } else if (msg.startsWith('video')) {
        setVideoUrl(msg.split(' ')[1]);
      }
    },
    [setOverlay, setVideoUrl],
  );

  useEffect(() => {
    addMessageCallback('player', messageCallback);
    return () => removeMessageCallback('player');
  }, [messageCallback, addMessageCallback, removeMessageCallback]);

  return (
    <div className="aspect-ratio">
      {videoUrl === null ? (
        <div className="aspect-ratio-inner empty-player">NO VIDEO</div>
      ) : (
        <div className="aspect-ratio-inner">
          {isYoutubeUrl(videoUrl) ? (
            <YoutubePlayer
              addMessageCallback={addMessageCallback}
              removeMessageCallback={removeMessageCallback}
              videoUrl={videoUrl}
              sendMessage={sendMessage}
              setOverlay={setOverlay}
              volume={volume}
              setVolume={setVolume}
              initialized={initialized}
              setInitialized={setInitialized}
            />
          ) : (
            <VideoJsPlayer
              addMessageCallback={addMessageCallback}
              removeMessageCallback={removeMessageCallback}
              videoUrl={videoUrl}
              sendMessage={sendMessage}
              setOverlay={setOverlay}
              volume={volume}
              setVolume={setVolume}
              initialized={initialized}
              setInitialized={setInitialized}
            />
          )}
          {getOverlay(overlay)}
        </div>
      )}
    </div>
  );
}

export default Player;
