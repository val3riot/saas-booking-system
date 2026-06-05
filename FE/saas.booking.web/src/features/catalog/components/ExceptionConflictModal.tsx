import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { StatusMessage } from '../../../components/StatusMessage';

type ExceptionConflictModalProps = {
  conflict: {
    createdCount: number;
    failedItem: { label: string };
    message: string;
    remainingItems: unknown[];
  };
  isSaving: boolean;
  onClose: () => void;
  onContinue: () => void;
};

export function ExceptionConflictModal({ conflict, isSaving, onClose, onContinue }: ExceptionConflictModalProps) {
  return (
    <Modal
      title="Indisponibilita non creata"
      eyebrow="Conflitto agenda"
      description={`Il servizio ${conflict.failedItem.label} non puo essere aggiornato con questa indisponibilita.`}
      onClose={onClose}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="ghost" type="button" onClick={onClose} disabled={isSaving}>
            Chiudi
          </Button>
          <Button type="button" onClick={onContinue} disabled={isSaving}>
            {conflict.remainingItems.length === 0 ? 'Conferma' : 'Salta e continua'}
          </Button>
        </div>
      }
    >
      <StatusMessage tone="warning">{conflict.message}</StatusMessage>

      <dl className="grid gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Create finora</dt>
          <dd className="font-semibold text-slate-950">{conflict.createdCount}</dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Ancora da processare</dt>
          <dd className="font-semibold text-slate-950">{conflict.remainingItems.length}</dd>
        </div>
      </dl>
    </Modal>
  );
}
