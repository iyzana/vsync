import './Player.css';
import { faPause, faSyncAlt } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useState } from 'react';
import YoutubePlayer from './YoutubePlayer';
import VideoJsPlayer from './VideoJsPlayer';
import { useWebsocketMessages } from '../hook/websocket-messages';

function getOverlay(overlay: 'PAUSED' | 'SYNCING' | null) {
  if (overlay == null) {
    return null;
  }

  const icon = overlay === 'PAUSED' ? faPause : faSyncAlt;
  const spin = overlay === 'SYNCING';

  return (
    <div className="aspect-ratio-inner overlay">
      <FontAwesomeIcon icon={icon} spin={spin} />
      <div>{overlay}</div>
    </div>
  );
}

export interface EmbeddedPlayerProps {
  videoUrl: string;
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

function Player() {
  const [overlay, setOverlay] = useState<'PAUSED' | 'SYNCING' | null>(null);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [volume, setVolume] = useState<number | null>(null);
  const [initialized, setInitialized] = useState<boolean>(false);

  useWebsocketMessages((msg: string) => {
    if (msg === 'play') {
      setOverlay(null);
    } else if (msg.startsWith('pause')) {
      setOverlay('PAUSED');
    } else if (msg.startsWith('ready?')) {
      setOverlay('SYNCING');
    } else if (msg.startsWith('video')) {
      setVideoUrl(msg.split(' ').slice(1).join(' '));
    }
  }, []);

  return (
    <div className="aspect-ratio">
      {videoUrl === null ? (
        <div className="aspect-ratio-inner empty-player">NO VIDEO</div>
      ) : (
        <div className="aspect-ratio-inner">
          {isYoutubeUrl(videoUrl) ? (
            <YoutubePlayer
              videoUrl={videoUrl}
              setOverlay={setOverlay}
              volume={volume}
              setVolume={setVolume}
              initialized={initialized}
              setInitialized={setInitialized}
            />
          ) : (
            <VideoJsPlayer
              videoUrl={videoUrl}
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
