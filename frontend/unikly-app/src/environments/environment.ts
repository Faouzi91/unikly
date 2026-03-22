export const environment = {
  production: false,
  apiUrl: '/api',
  wsUrl: '/ws',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'unikly',
    clientId: 'unikly-frontend',
  },
  // Stripe TEST publishable key — get from https://dashboard.stripe.com/test/apikeys
  stripePublishableKey: 'pk_test_51TDbpkCVmfTR414iZSGQbxGDzS61OSYW8LC36V3Su522Z8e9yHBwwgXyDlRlzck9m9VcqnbYZiLanir4wsWswroO00xsADbK3l',
};
