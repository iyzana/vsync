import './Player.css';
import { faPause, faSyncAlt } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useEffect, useState } from 'react';
import YoutubePlayer from './YoutubePlayer';

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
  msg: string | null;
  sendMessage: (message: string) => void;
}

function Player({ msg, sendMessage }: PlayerProps) {
  const [overlay, setOverlay] = useState<'PAUSED' | 'SYNCING' | null>(null);
  const [videoUrl, setVideoUrl] = useState<string>('');

  useEffect(() => {
    if (!msg) {
      return;
    } else if (msg === 'play') {
      setOverlay(null);
    } else if (msg.startsWith('pause')) {
      setOverlay('PAUSED');
    } else if (msg.startsWith('ready?')) {
      setOverlay('SYNCING');
    } else if (msg.startsWith('video')) {
      setVideoUrl(msg.split(' ')[1]);
    }
  }, [msg, setOverlay, setVideoUrl]);

  return (
    <div className="aspect-ratio">
      {videoUrl === null || videoUrl === '' || videoUrl === undefined ? (
        <div className="aspect-ratio-inner empty-player">NO VIDEO</div>
      ) : (
        <div className="aspect-ratio-inner">
          <YoutubePlayer
            msg={msg}
            videoUrl={videoUrl}
            sendMessage={sendMessage}
            setOverlay={setOverlay}
          />
          {getOverlay(overlay)}
        </div>
      )}
    </div>
  );
}

export default Player;
