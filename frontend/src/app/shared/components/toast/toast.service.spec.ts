import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    service = new ToastService();
  });

  it('adds success and error toasts', () => {
    service.success('common.saved');
    service.error('errors.UNKNOWN');

    const toasts = service.toasts();
    expect(toasts.length).toBe(2);
    expect(toasts[0].kind).toBe('success');
    expect(toasts[1].kind).toBe('error');
  });

  it('dismisses a toast by id', () => {
    service.success('common.saved');
    const id = service.toasts()[0].id;

    service.dismiss(id);

    expect(service.toasts().length).toBe(0);
  });
});
