import React from 'react';
import './YtEmbed.css';
import YouTube, { Options } from 'react-youtube';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPause, faSyncAlt } from '@fortawesome/free-solid-svg-icons';

interface YtEmbedProps {
  videoId: string;
  onStateChange: () => void;
  onPlaybackRateChange: (event: { target: any; data: number }) => void;
  setPlayer: (player: any) => void;
  overlay: 'PAUSED' | 'SYNCING' | null;
}

const opts: Options = {
  width: '100%',
  height: '100%',
  playerVars: {
    autoplay: 1,
    modestbranding: 1,
    rel: 0,
  },
};

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

function YtEmbed({
  videoId,
  onStateChange,
  onPlaybackRateChange,
  setPlayer,
  overlay,
}: YtEmbedProps) {
  return (
    <div className="aspect-ratio">
      {videoId === '' ? (
        <div className="aspect-ratio-inner empty-player">NO VIDEO</div>
      ) : (
        <>
          <YouTube
            opts={opts}
            containerClassName="aspect-ratio-inner"
            videoId={videoId}
            onReady={(e) => setPlayer(e.target)}
            onStateChange={onStateChange}
            onPlaybackRateChange={onPlaybackRateChange}
          ></YouTube>
          {getOverlay(overlay)}
        </>
      )}
    </div>
  );
}

export default YtEmbed;
