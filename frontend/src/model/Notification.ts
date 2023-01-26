export default interface Notification {
  message: string;
  level: 'info' | 'error';
  permanent: boolean;
}
