import './Player.css';
import { useCallback, useEffect, useState, lazy, Suspense } from 'react';
import {
  useWebsocketClose,
  useWebsocketMessages,
} from '../hook/websocket-messages';
import Overlay from './Overlay';
import ErrorBoundary from './ErrorBoundary';
import OverlayState from '../model/Overlay';
import { VideoCommand } from '../model/VideoSource';
const YoutubePlayer = lazy(() => import('./YoutubePlayer'));
const VideoJsPlayer = lazy(() => import('./VideoJsPlayer'));

export interface EmbeddedPlayerProps {
  video: VideoCommand;
  setOverlay: (state: OverlayState) => void;
  volume: number | null;
  setVolume: (volume: number) => void;
  playbackPermission: boolean;
  gotPlaybackPermission: () => void;
}

function isYoutubeUrl(url: string): boolean {
  return (
    url.startsWith('https://www.youtube.com/') ||
    url.startsWith('https://youtu.be/')
  );
}

function Player() {
  const [overlay, setOverlay] = useState<OverlayState>(OverlayState.NONE);
  const [video, setSource] = useState<VideoCommand | null>(null);
  const [volume, setVolume] = useState<number | null>(null);
  const [playbackPermission, setPlaybackPermission] = useState<boolean>(false);

  useWebsocketMessages(
    (msg: string) => {
      if (msg === 'play') {
        setOverlay(OverlayState.NONE);
      } else if (msg.startsWith('pause')) {
        setOverlay(OverlayState.PAUSED);
      } else if (msg.startsWith('ready?')) {
        setOverlay(OverlayState.SYNCING);
      } else if (msg === 'video') {
        if (overlay !== OverlayState.UNSTARTED) {
          setOverlay(OverlayState.NONE);
        }
      } else if (msg.startsWith('video')) {
        setSource(JSON.parse(msg.split(' ').slice(1).join(' ')));
      }
    },
    [overlay],
  );

  useWebsocketClose(() => {
    if (document.fullscreenElement !== null) {
      document.exitFullscreen();
    }
  }, []);

  useEffect(() => {
    if (playbackPermission || overlay !== OverlayState.NONE) {
      return;
    }
    const timeout = setTimeout(() => {
      console.log('set unstarted overlay');
      setOverlay(OverlayState.UNSTARTED);
    }, 1000);
    return () => clearTimeout(timeout);
  }, [playbackPermission, overlay]);

  const gotPlaybackPermission = useCallback(() => {
    setPlaybackPermission(true);
    if (overlay === OverlayState.UNSTARTED) {
      setOverlay(OverlayState.NONE);
    }
  }, [overlay]);

  return (
    <div className="aspect-ratio">
      {video === null ? (
        <div className="aspect-ratio-inner empty-player">
          No videos in queue
        </div>
      ) : (
        <ErrorBoundary
          fallback={(message) => (
            <div className="aspect-ratio-inner empty-player">
              Failed to load player: {message}
            </div>
          )}
        >
          <Suspense
            fallback={
              <div className="aspect-ratio-inner empty-player">
                Loading player
              </div>
            }
          >
            <div className="aspect-ratio-inner">
              {isYoutubeUrl(video.source.url) ? (
                <YoutubePlayer
                  video={video}
                  setOverlay={setOverlay}
                  volume={volume}
                  setVolume={setVolume}
                  playbackPermission={playbackPermission}
                  gotPlaybackPermission={gotPlaybackPermission}
                />
              ) : (
                <VideoJsPlayer
                  video={video}
                  setOverlay={setOverlay}
                  volume={volume}
                  setVolume={setVolume}
                  playbackPermission={playbackPermission}
                  gotPlaybackPermission={gotPlaybackPermission}
                />
              )}
              <Overlay state={overlay} />
            </div>
          </Suspense>
        </ErrorBoundary>
      )}
    </div>
  );
}

export default Player;
