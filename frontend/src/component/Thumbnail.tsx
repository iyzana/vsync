import { faImage, faSlash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import './Thumbnail.css';

interface ThumbnailProps {
  thumbnailUrl: string | null;
}

function Thumbnail({ thumbnailUrl }: ThumbnailProps) {
  return thumbnailUrl === null ? (
    <div className="thumbnail placeholder">
      <span className="fa-layers fa-fw">
        <FontAwesomeIcon icon={faSlash} mask={faImage} transform={{ y: 2.0 }} />
        <FontAwesomeIcon icon={faSlash} />
      </span>
    </div>
  ) : (
    <img className="thumbnail" src={thumbnailUrl || undefined} alt="" />
  );
}

export default Thumbnail;
