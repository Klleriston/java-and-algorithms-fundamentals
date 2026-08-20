import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Catalog } from './pages/Catalog';
import { DemoScreen } from './pages/DemoScreen';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Catalog />} />
        <Route path="/demo/:id" element={<DemoScreen />} />
      </Routes>
    </BrowserRouter>
  );
}
