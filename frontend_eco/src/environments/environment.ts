export const environment = {
  production: true,
  /** Tất cả request REST đi qua API Gateway (Spring Cloud Gateway, cổng 8000). */
  apiUrl: 'http://localhost:8000/api/v1',
  /** WebSocket nối thẳng vào order-service: Gateway không proxy được SockJS. */
  wsOrdersUrl: 'http://localhost:8082/ws-orders',
};
