import React from 'react';
import './YtEmbed.css';
import YouTube, { Options } from 'react-youtube';

interface YtEmbedProps {
  videoId: string;
  onStateChange: () => void;
  setPlayer: (player: any) => void;
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

function YtEmbed({ videoId, onStateChange, setPlayer }: YtEmbedProps) {
  return (
    <div className="aspect-ratio">
      {videoId === '' ? (
        <div className="aspect-ratio-inner empty-player">NO VIDEO</div>
      ) : (
        <YouTube
          opts={opts}
          containerClassName="aspect-ratio-inner"
          videoId={videoId}
          onReady={e => setPlayer(e.target)}
          onStateChange={onStateChange}
        ></YouTube>
      )}
    </div>
  );
}

export default YtEmbed;
