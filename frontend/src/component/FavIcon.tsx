import { useState } from 'react';
import QueueItem from '../model/QueueItem';
import './FavIcon.css';

const faviconUrl = (url: string, originalQuery: string) => {
  let baseUrl;
  try {
    baseUrl = new URL(originalQuery);
  } catch (e) {
    baseUrl = new URL(url);
  }
  baseUrl.search = '';
  baseUrl.pathname = 'favicon.ico';
  return baseUrl;
};

interface FavIconProps {
  item: QueueItem;
}

function FavIcon({ item }: FavIconProps) {
  const [error, setError] = useState(false);
  const favicon = faviconUrl(item.source.url, item.originalQuery);
  return error ? null : (
    <img
      className="favicon"
      src={favicon.toString()}
      alt={`favicon of ${favicon.host}`}
      onError={() => setError(true)}
    ></img>
  );
}

export default FavIcon;
