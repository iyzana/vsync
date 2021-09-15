import React, { useEffect, useState } from 'react';
import './Input.css';
import Error from './Error';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

interface InputProps {
  ws: WebSocket;
  errors: Error[];
  setErrors: (map: (errors: Error[]) => Error[]) => void;
}

function Input({ ws, errors, setErrors }: InputProps) {
  const [message, setMessage] = useState('');

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      send();
    }
  };

  const send = () => {
    if (message.trim() !== '') {
      ws.send(`queue add ${message}`);
      setMessage('');
    }
  };

  useEffect(() => {
    if (errors.length === 0) {
      return;
    }
    const timeout = setTimeout(() => {
      setErrors((errors) => errors.filter((error) => error.permanent));
    }, 3000);
    return () => clearTimeout(timeout);
  }, [errors, setErrors]);
  return (
    <div>
      {errors.map((error, index) => (
        <div key={index} className="error">
          {error.message}
        </div>
      ))}
      <div className="input-group">
        <input
          className="input-text"
          type="text"
          placeholder="Enter YouTube URL here"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyUp={onKey}
        />
        <button className="input-send" onClick={send} alia-label="Add to queue">
          <FontAwesomeIcon icon={faPlus} />
        </button>
      </div>
    </div>
  );
}

export default Input;
