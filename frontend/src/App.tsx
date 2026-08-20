import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { LanguageToggle } from './components/LanguageToggle';
import { Catalog } from './pages/Catalog';
import { DemoScreen } from './pages/DemoScreen';

export function App() {
  return (
    <BrowserRouter>
      <header className="app-header">
        <LanguageToggle />
      </header>
      <Routes>
        <Route path="/" element={<Catalog />} />
        <Route path="/demo/:id" element={<DemoScreen />} />
      </Routes>
    </BrowserRouter>
  );
}
