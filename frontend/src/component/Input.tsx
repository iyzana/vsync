import React, { useState } from 'react';
import './Input.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircleNotch, faPlus } from '@fortawesome/free-solid-svg-icons';

interface InputProps {
  addToQueue: (input: string) => void;
  working: boolean;
}

function Input({ addToQueue, working }: InputProps) {
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

  return (
    <div className="input-group">
      <input
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
        alia-label="Add to queue"
        disabled={working}
      >
        <FontAwesomeIcon
          icon={working ? faCircleNotch : faPlus}
          spin={working}
        />
      </button>
    </div>
  );
}

export default Input;
