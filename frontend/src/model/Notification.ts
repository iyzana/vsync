export default interface Notification {
  message: string;
  level: 'success' | 'info' | 'error';
  permanent: boolean;
}
