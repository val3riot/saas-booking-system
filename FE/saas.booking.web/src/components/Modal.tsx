import { ReactNode, useEffect } from 'react';
import { Button } from './Button';

type ModalProps = {
  title: string;
  eyebrow?: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  onClose: () => void;
};

export function Modal({ title, eyebrow, description, children, footer, onClose }: ModalProps) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/45 p-4">
      <section
        className="grid w-full max-w-lg gap-4 rounded-lg bg-white p-5 shadow-xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <div>
          {eyebrow && <p className="mb-1 text-xs font-bold uppercase text-brand-600">{eyebrow}</p>}
          <h2 id="modal-title" className="text-xl font-bold text-slate-950">
            {title}
          </h2>
          {description && <p className="text-slate-500">{description}</p>}
        </div>
        {children}
        {footer ?? (
          <div className="flex justify-end">
            <Button variant="ghost" type="button" onClick={onClose}>
              Chiudi
            </Button>
          </div>
        )}
      </section>
    </div>
  );
}
