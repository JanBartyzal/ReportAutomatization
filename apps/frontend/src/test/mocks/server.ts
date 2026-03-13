import { setupServer } from 'msw/node';
import { handlers } from './handlers';

// Create the MSW server with all handlers
export const server = setupServer(...handlers);

// Export handlers for individual test customization
export { handlers };
