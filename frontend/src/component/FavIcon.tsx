import { useState } from 'react';
import QueueItem from '../model/QueueItem';
import './FavIcon.css';

interface FavIconProps {
  item: QueueItem;
}

function FavIcon({ item }: FavIconProps) {
  const [error, setError] = useState(false);
  return error || !item.favicon ? null : (
    <img
      className="favicon"
      src={item.favicon}
      alt={`favicon of ${new URL(item.favicon).host}`}
      onError={() => setError(true)}
    ></img>
  );
}

export default FavIcon;
