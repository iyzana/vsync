import './Player.css';
import { useCallback, useState } from 'react';
import YoutubePlayer from './YoutubePlayer';
import VideoJsPlayer from './VideoJsPlayer';
import { useWebsocketMessages } from '../hook/websocket-messages';
import Overlay from './Overlay';
import OverlayState from '../model/Overlay';

export interface EmbeddedPlayerProps {
  videoUrl: string;
  setOverlay: (state: OverlayState) => void;
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
  const [overlay, setOverlay] = useState<OverlayState>(OverlayState.NONE);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [volume, setVolume] = useState<number | null>(null);
  const [initialized, setInitialized] = useState<boolean>(false);

  useWebsocketMessages(
    (msg: string) => {
      if (msg === 'play') {
        setOverlay(OverlayState.NONE);
      } else if (msg.startsWith('pause')) {
        setOverlay(OverlayState.PAUSED);
      } else if (msg.startsWith('ready?')) {
        setOverlay(OverlayState.SYNCING);
      } else if (msg === 'video') {
        setOverlay(initialized ? OverlayState.NONE : OverlayState.UNSTARTED);
      } else if (msg.startsWith('video')) {
        setVideoUrl(msg.split(' ').slice(1).join(' '));
      }
    },
    [initialized],
  );

  const init = useCallback(
    (initialized: boolean) => {
      setInitialized(initialized);
      if (overlay === OverlayState.UNSTARTED) {
        setOverlay(OverlayState.NONE);
      }
    },
    [overlay],
  );

  return (
    <div className="aspect-ratio">
      {videoUrl === null ? (
        <div className="aspect-ratio-inner empty-player">
          No videos in queue
        </div>
      ) : (
        <div className="aspect-ratio-inner">
          {isYoutubeUrl(videoUrl) ? (
            <YoutubePlayer
              videoUrl={videoUrl}
              setOverlay={setOverlay}
              volume={volume}
              setVolume={setVolume}
              initialized={initialized}
              setInitialized={init}
            />
          ) : (
            <VideoJsPlayer
              videoUrl={videoUrl}
              setOverlay={setOverlay}
              volume={volume}
              setVolume={setVolume}
              initialized={initialized}
              setInitialized={init}
            />
          )}
          <Overlay state={overlay} />
        </div>
      )}
    </div>
  );
}

export default Player;
