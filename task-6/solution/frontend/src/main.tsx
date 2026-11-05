import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';

type JobResult = { executionId: number | null; status: string; traceId: string | null; message: string | null };

function App() {
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<JobResult | null>(null);
  const [error, setError] = useState('');

  async function startJob() {
    setRunning(true);
    setResult(null);
    setError('');
    try {
      const response = await fetch('/jobs/import', { method: 'POST' });
      const data: JobResult = await response.json();
      setResult(data);
      if (!response.ok) setError(data.message || 'Задание завершилось с ошибкой.');
    } catch {
      setError('Не удалось получить ответ от сервера. Проверьте доступность приложения.');
    } finally {
      setRunning(false);
    }
  }

  return <div className="shell">
    <header><a className="brand" href="/"><span className="brand-mark">T</span> TRADEWARE<span className="brand-dot">.</span></a><span className="header-label">СИСТЕМА УПРАВЛЕНИЯ ДАННЫМИ</span></header>
    <main>
      <div className="breadcrumb">ОПЕРАЦИИ <span>/</span> ОБРАБОТКА ДАННЫХ</div>
      <div className="page-heading"><div><span className="eyebrow">СКЛАДСКИЕ ОПЕРАЦИИ</span><h1>Обработка отчёта</h1><p>Загрузите данные о товарах и проследите результат выполнения.</p></div><div className="environment"><span className="pulse"/> ЛОКАЛЬНОЕ ОКРУЖЕНИЕ</div></div>
      <div className="layout">
        <section className="card main-card"><div className="section-top"><span className="icon">↗</span><span className="pill">SPRING BATCH</span></div><h2>Импорт товаров</h2><p>Обработка запускает подготовленный CSV-файл, дополняет товары данными программы лояльности и сохраняет результат в базу.</p>
          <div className="file-info"><span className="file-icon">▤</span><span><strong>product-data.csv</strong><small>Исходный файл · CSV</small></span><span className="file-label">ГОТОВ К ОБРАБОТКЕ</span></div>
          <button type="button" onClick={startJob} disabled={running}>{running ? 'Обработка выполняется…' : 'Запустить обработку'} <span>→</span></button>
          <div className="hint">Обработка синхронная. Не закрывайте страницу до получения результата.</div>
        </section>
        <aside className="card result-card"><div className="result-heading"><span>РЕЗУЛЬТАТ ЗАПУСКА</span><span className="tiny-dot"/></div>
          {running ? <div className="state" role="status"><div className="state-symbol spinner">↻</div><h3>Выполняется обработка</h3><p>Ожидаем ответ сервера.</p></div> : result ? <div className="result-content" role="status"><div className={error ? 'status error' : 'status success'}><span>●</span> {error ? 'ОШИБКА ОБРАБОТКИ' : 'УСПЕШНО ЗАВЕРШЕНО'}</div><h3>{error ? 'Не удалось обработать отчёт' : 'Отчёт обработан'}</h3><p>{error || 'Результат сохранён в базе данных.'}</p><dl><div><dt>Статус</dt><dd>{result.status}</dd></div><div><dt>Номер запуска</dt><dd>{result.executionId ?? '—'}</dd></div><div className="trace"><dt>Trace ID <span>ДЛЯ ПОИСКА В KIBANA</span></dt><dd>{result.traceId ?? 'Не получен'}</dd></div></dl></div> : error ? <div className="state" role="alert"><div className="state-symbol failed">!</div><h3>Нет ответа от сервера</h3><p>{error}</p></div> : <div className="state"><div className="state-symbol">◎</div><h3>Пока нет запусков</h3><p>Запустите обработку, чтобы увидеть её статус и идентификатор трассировки.</p></div>}
        </aside>
      </div>
      <div className="flow"><span className="flow-label">КАК ЭТО РАБОТАЕТ</span><div className="steps"><div><b>01</b><span>Чтение CSV<small>Исходные данные о товарах</small></span></div><i>→</i><div><b>02</b><span>Обогащение<small>Программа лояльности</small></span></div><i>→</i><div><b>03</b><span>Сохранение<small>Запись в PostgreSQL</small></span></div></div></div>
    </main><footer><span>TRADEWARE <b> / </b> DATA OPERATIONS</span><span>DEMO · TASK 06</span></footer>
  </div>;
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);
