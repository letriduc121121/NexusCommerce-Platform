export const environment = {
  production: false,
  /** API Gateway chạy local ở cổng 8000. */
  apiUrl: 'http://localhost:8000/api/v1',
  /** WebSocket nối thẳng vào order-service: Gateway không proxy được SockJS. */
  wsOrdersUrl: 'http://localhost:8082/ws-orders',
};
