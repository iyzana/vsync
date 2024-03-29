import { faXmark } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  ReactNode,
  SyntheticEvent,
  useCallback,
  useEffect,
  useRef,
} from 'react';
import './Dialog.css';

interface DialogProps {
  open: boolean;
  children: ReactNode;
  onDismiss?: () => void;
}

function Dialog({ open, children, onDismiss }: DialogProps) {
  let dialogRef = useRef<HTMLDialogElement | null>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }
    if (open && !dialog.open) {
      dialog.showModal();
      dialog.classList.add('animate');
    } else if (!open && dialog.open) {
      dialog.classList.remove('animate');
      setTimeout(() => dialog.close(), 100);
    }
  }, [open]);

  const dismiss = useCallback(
    (event: SyntheticEvent<Element, Event>) => {
      event.preventDefault();
      if (onDismiss) {
        onDismiss();
      }
    },
    [onDismiss],
  );

  const onClick = useCallback(
    (event: SyntheticEvent<HTMLDialogElement, Event>) => {
      if (event.target === dialogRef.current) {
        dismiss(event);
      }
    },
    [dismiss],
  );

  return (
    <dialog ref={dialogRef} onClick={onClick} onCancel={dismiss}>
      <div>
        <button className="close" onClick={dismiss} aria-label="Close dialog">
          <FontAwesomeIcon icon={faXmark} size="lg" />
        </button>
        {children}
      </div>
    </dialog>
  );
}

export default Dialog;
