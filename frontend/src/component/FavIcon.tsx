import { useEffect, useState } from 'react';
import './FavIcon.css';

interface FavIconProps {
  favicon?: string;
}

function FavIcon({ favicon }: FavIconProps) {
  const [error, setError] = useState(false);
  useEffect(() => setError(false), [favicon]);
  return error || !favicon ? null : (
    <img
      className="favicon"
      src={favicon}
      alt={`favicon of ${new URL(favicon).host}`}
      onError={() => setError(true)}
    ></img>
  );
}

export default FavIcon;
