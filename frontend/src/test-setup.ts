import { afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';

// The language now persists, so a test that switches it would leak into the next
// file and silently render everything in the other language.
afterEach(() => localStorage.clear());
