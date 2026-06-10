/**
 * uuidv4.js — Local RFC 4122 v4 UUID generator for k6 scripts.
 *
 * Replaces the https://jslib.k6.io/k6-utils/1.4.0/index.js remote import so
 * scripts run in air-gapped CI without outbound fetches at script init time.
 *
 * Implementation: pure JS using Math.random() — sufficient for generating
 * non-colliding idempotency keys in a smoke test with low VU count.
 */

/**
 * Generate a RFC 4122 version 4 UUID.
 *
 * @returns {string} e.g. "550e8400-e29b-41d4-a716-446655440000"
 */
export function uuidv4() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
