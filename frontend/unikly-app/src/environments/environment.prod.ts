export const environment = {
  production: true,
  apiUrl: '/api',
  wsUrl: '/ws',
  keycloak: {
    url: '/auth',
    realm: 'unikly',
    clientId: 'unikly-frontend',
  },
  // Stripe LIVE publishable key — get from https://dashboard.stripe.com/apikeys (disable Test Mode)
  stripePublishableKey: 'pk_live_REPLACE_WITH_YOUR_LIVE_KEY',
};
