import {
  faCircleNotch,
  faImage,
  faSlash,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import './Thumbnail.css';

interface ThumbnailProps {
  thumbnailUrl: string | null;
  loading: boolean;
}

function Thumbnail({ thumbnailUrl, loading }: ThumbnailProps) {
  return thumbnailUrl === null ? (
    <div className="thumbnail placeholder">
      {loading ? (
        <FontAwesomeIcon icon={faCircleNotch} spin={true} />
      ) : (
        <span className="fa-layers fa-fw">
          <FontAwesomeIcon
            icon={faSlash}
            mask={faImage}
            transform={{ y: 2.0 }}
          />
          <FontAwesomeIcon icon={faSlash} />
        </span>
      )}
    </div>
  ) : (
    <div className='thumbnail'>
      <img src={thumbnailUrl || undefined} alt="" />
    </div>
  );
}

export default Thumbnail;
