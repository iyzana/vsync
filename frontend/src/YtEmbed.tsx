import React from 'react';
import './YtEmbed.css';
import YouTube, { Options } from 'react-youtube';

interface YtEmbedProps {
  videoId: string;
  onStateChange: () => void;
  setPlayer: (player: any) => void;
  overlayText: string | null;
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

function YtEmbed({
  videoId,
  onStateChange,
  setPlayer,
  overlayText,
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
          ></YouTube>
          {overlayText ? (
            <div className="aspect-ratio-inner overlay">{overlayText}</div>
          ) : null}
        </>
      )}
    </div>
  );
}

export default YtEmbed;
