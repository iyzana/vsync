import React, { useState } from 'react';
import './Input.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

interface InputProps {
  addToQueue: (input: string) => void;
}

function Input({ addToQueue }: InputProps) {
  const [input, setInput] = useState('');

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      send();
    }
  };

  const send = () => {
    if (input.trim() !== '') {
      addToQueue(input);
      setInput('');
    }
  };

  // autofocus input in new rooms
  const autoFocus = window.location.pathname === '/';

  return (
    <div className="input-group">
      <input
        autoFocus={autoFocus}
        className="input-text"
        type="text"
        placeholder="Video URL or YouTube search query"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyUp={onKey}
      />
      <button
        className="input-send"
        onClick={send}
        aria-label="Add to queue"
      >
        <FontAwesomeIcon icon={faPlus} />
      </button>
    </div>
  );
}

export default Input;
