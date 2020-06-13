import React from 'react';
import './Queue.css';
import QueueItem from './QueueItem';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTimes, faAngleDoubleRight } from '@fortawesome/free-solid-svg-icons';

interface QueueProps {
  videos: QueueItem[];
  removeVideo: (videoId: string) => void;
  skip: () => void;
  numUsers: number;
}

function Queue({ videos, removeVideo, skip, numUsers }: QueueProps) {
  return (
    <div className="queue">
      <div className="header">
        <h3 className="title">Queue</h3>
        <div>
          {videos.length === 0 ? null : (
            <button className="skip" onClick={skip}>
              SKIP <FontAwesomeIcon icon={faAngleDoubleRight} />
            </button>
          )}
          <span style={{ display: 'inline-block', marginTop: 'var(--s0)' }}>
            {numUsers + ' connected'}
          </span>
        </div>
      </div>
      <ol className="queue-list">
        {videos.map(({ videoId, title, thumbnail }) => (
          <li key={videoId} className="queue-item">
            <div className="video-info">
              <img className="thumbnail" src={thumbnail} alt="" />
              <div>{title}</div>
            </div>
            <button className="remove" onClick={() => removeVideo(videoId)}>
              <FontAwesomeIcon icon={faTimes} />
            </button>
          </li>
        ))}
      </ol>
    </div>
  );
}

export default Queue;
