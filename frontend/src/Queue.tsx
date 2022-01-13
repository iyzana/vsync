import React from 'react';
import './Queue.css';
import QueueItem from './QueueItem';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTimes, faAngleDoubleRight } from '@fortawesome/free-solid-svg-icons';
import { faClipboard } from '@fortawesome/free-regular-svg-icons';
import { ReactSortable } from 'react-sortablejs';

interface QueueProps {
  videos: QueueItem[];
  setVideos: (videos: QueueItem[]) => void;
  removeVideo: (videoId: string) => void;
  skip: () => void;
  numUsers: number;
}

function copyLink() {
  if (!navigator.clipboard) {
    return;
  }
  navigator.clipboard.writeText(window.location.href);
}

function Queue({ videos, setVideos, removeVideo, skip, numUsers }: QueueProps) {
  return (
    <div className="queue">
      <div className="header">
        <h3 className="title">Queue</h3>
        <div className="status">
          {videos.length === 0 ? null : (
            <button className="skip" onClick={skip}>
              SKIP <FontAwesomeIcon icon={faAngleDoubleRight} />
            </button>
          )}
          <span className="connections">
            {numUsers === 1
              ? 'no one else watching'
              : `${numUsers} humans watching`}
          </span>
          <span className="getlink" onClick={copyLink}>
            <FontAwesomeIcon icon={faClipboard} />
          </span>
        </div>
      </div>
      <ReactSortable list={videos} setList={setVideos} className="queue-list">
        {videos.map(({ id, title, thumbnail }) => (
          <li key={id} className="queue-item">
            <div className="video-info">
              <img className="thumbnail" src={thumbnail} alt="" />
              <div>{title}</div>
            </div>
            <button className="remove" onClick={() => removeVideo(id)}>
              <FontAwesomeIcon icon={faTimes} />
            </button>
          </li>
        ))}
      </ReactSortable>
    </div>
  );
}

export default Queue;
