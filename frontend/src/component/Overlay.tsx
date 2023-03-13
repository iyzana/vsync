import {
  faCircleExclamation,
  faPause,
  faSyncAlt,
  IconDefinition,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import OverlayState from '../model/Overlay';
import './Overlay.css';

export interface OverlayConfig {
  text: string;
  icon: IconDefinition | null;
  classes: string | null;
}

const OVERLAYS: Record<keyof typeof OverlayState, OverlayConfig | null> = {
  NONE: null,
  PAUSED: {
    text: 'Paused',
    icon: faPause,
    classes: null,
  },
  SYNCING: {
    text: 'Syncing',
    icon: faSyncAlt,
    classes: null,
  },
  UNSTARTED: {
    text: 'Click to allow playback',
    icon: faCircleExclamation,
    classes: 'blocked',
  },
};

export interface OverlayProps {
  state: OverlayState;
}

function Overlay({ state }: OverlayProps) {
  const config = OVERLAYS[state];

  if (config == null) {
    return null;
  }

  const { text, icon, classes } = config;
  const spin = state === OverlayState.SYNCING;

  return (
    <div className={`aspect-ratio-inner overlay ${classes || ''}`}>
      <div className="text">
        {icon ? <FontAwesomeIcon icon={icon} spin={spin} /> : null}
        {text}
      </div>
    </div>
  );
}

export default Overlay;
