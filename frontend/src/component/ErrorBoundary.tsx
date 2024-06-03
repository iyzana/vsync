import { ReactNode, Component } from 'react';

interface ErrorBoundaryProps {
  fallback: (message: string) => ReactNode;
  children: ReactNode;
}

export default class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  { error: string | null }
> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: any) {
    if (typeof error === 'string') {
      return { error };
    } else if (error instanceof Error) {
      return { error: error.name };
    } else {
      return { error: 'Unknown Error' };
    }
  }

  render() {
    if (this.state.error) {
      return this.props.fallback(this.state.error);
    }
    return this.props.children;
  }
}
