import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Typography,
  Alert,
  Paper,
  List,
  ListItem,
  ListItemText,
  Divider,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material';
import { CloudUpload as CloudUploadIcon, Download as DownloadIcon } from '@mui/icons-material';
import { ImportApi, ImportResult, ImportHistory } from '../services/api';

interface ImportDialogProps {
  onImportComplete: () => void;
}

const loadHistory = async (setHistory: (history: ImportHistory[]) => void, setHistoryLoading: (loading: boolean) => void) => {
  setHistoryLoading(true);
  try {
    const historyData = await ImportApi.getImportHistory();
    setHistory(historyData);
  } catch (err) {
    console.error('Failed to load import history:', err);
  } finally {
    setHistoryLoading(false);
  }
};

const ImportDialog: React.FC<ImportDialogProps> = ({ onImportComplete }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [history, setHistory] = useState<ImportHistory[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  const formatDate = (date: Date | string | number | undefined) => {
    if (!date) return 'N/A';
    try {
      let dateObj: Date;
      if (typeof date === 'number') {
        dateObj = new Date(date);
      } else if (typeof date === 'string') {
        dateObj = new Date(date);
      } else {
        dateObj = date;
      }
      
      if (isNaN(dateObj.getTime())) {
        return 'Invalid Date';
      }
      
      return dateObj.toLocaleDateString('ru-RU', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return 'Invalid Date';
    }
  };

  useEffect(() => {
    loadHistory(setHistory, setHistoryLoading);
  }, []);

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setError(null);
    setResult(null);
    setLoading(true);

    try {
      // Use new file upload API that saves file to MinIO
      const importResult = await ImportApi.importHumanBeingsFromFile(file);
      setResult(importResult);

      await loadHistory(setHistory, setHistoryLoading);

      if (importResult.success) {
        setTimeout(() => {
          onImportComplete();
        }, 2000);
      }
    } catch (err: any) {
      let errorMessage = 'Ошибка при импорте файла';
      
      if (err.response?.data?.message) {
        errorMessage = err.response.data.message;
      } else if (err.message) {
        errorMessage = err.message;
      } else if (err.response?.data?.data?.errors) {
        errorMessage = 'Validation errors: ' + err.response.data.data.errors.join(', ');
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadFile = async (importId: number) => {
    try {
      const blob = await ImportApi.downloadImportFile(importId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `import_${importId}.json`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: any) {
      console.error('Failed to download file:', err);
      alert('Ошибка при скачивании файла: ' + (err.message || 'Неизвестная ошибка'));
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Импорт HumanBeings из JSON
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {!result && (
        <Box>
          <input
            accept=".json"
            style={{ display: 'none' }}
            id="file-upload"
            type="file"
            onChange={handleFileUpload}
            disabled={loading}
          />
          <label htmlFor="file-upload">
            <Button
              variant="contained"
              component="span"
              startIcon={<CloudUploadIcon />}
              disabled={loading}
              fullWidth
            >
              {loading ? 'Загрузка...' : 'Выбрать JSON файл'}
            </Button>
          </label>
        </Box>
      )}

      {loading && (
        <Box display="flex" justifyContent="center" mt={2}>
          <CircularProgress />
        </Box>
      )}

      {result && (
        <Paper sx={{ mt: 2, p: 2 }}>
          <Typography variant="h6" gutterBottom>
            Результат импорта
          </Typography>

          <Alert severity={result.success ? 'success' : 'error'} sx={{ mb: 2 }}>
            {result.message}
          </Alert>

          <Typography variant="body1">
            Всего обработано: {result.data.totalProcessed}
          </Typography>
          <Typography variant="body1" color="success.main">
            Успешно импортировано: {result.data.successfullyImported}
          </Typography>
          {result.data.failed > 0 && (
            <Typography variant="body1" color="error.main">
              Ошибок: {result.data.failed}
            </Typography>
          )}

          {result.data.errors && result.data.errors.length > 0 && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Ошибки:
              </Typography>
              <List dense>
                {result.data.errors.map((err, index) => (
                  <React.Fragment key={index}>
                    <ListItem>
                      <ListItemText primary={err} />
                    </ListItem>
                    {index < result.data.errors!.length - 1 && <Divider />}
                  </React.Fragment>
                ))}
              </List>
            </Box>
          )}
        </Paper>
      )}

      {result && (
        <Box sx={{ mt: 2 }}>
          <Button variant="outlined" onClick={() => { setResult(null); setError(null); }}>
            Импортировать еще
          </Button>
        </Box>
      )}

      <Box sx={{ mt: 4 }}>
        <Typography variant="h6" gutterBottom>
          История импорта
        </Typography>
        
        {historyLoading ? (
          <Box display="flex" justifyContent="center" py={2}>
            <CircularProgress size={24} />
          </Box>
        ) : (
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Статус</TableCell>
                  <TableCell>Пользователь</TableCell>
                  <TableCell align="right">Добавлено</TableCell>
                  <TableCell align="right">Всего</TableCell>
                  <TableCell align="right">Ошибок</TableCell>
                  <TableCell>Дата</TableCell>
                  <TableCell>Файл</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {history.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      История импорта пуста
                    </TableCell>
                  </TableRow>
                ) : (
                  history.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>{item.id}</TableCell>
                      <TableCell>
                        <Chip 
                          label={item.status} 
                          color={item.status === 'SUCCESS' ? 'success' : 'error'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{item.username}</TableCell>
                      <TableCell align="right">
                        {item.status === 'SUCCESS' && item.addedCount !== null ? item.addedCount : '-'}
                      </TableCell>
                      <TableCell align="right">{item.totalProcessed}</TableCell>
                      <TableCell align="right">{item.failedCount}</TableCell>
                      <TableCell>
                        {formatDate(item.createdAt)}
                      </TableCell>
                      <TableCell>
                        {item.fileKey ? (
                          <Button
                            variant="outlined"
                            size="small"
                            startIcon={<DownloadIcon />}
                            onClick={() => handleDownloadFile(item.id)}
                          >
                            Скачать
                          </Button>
                        ) : (
                          <Typography variant="body2" color="textSecondary">
                            Нет файла
                          </Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        
        <Box sx={{ mt: 2 }}>
          <Button 
            variant="outlined" 
            size="small"
            onClick={() => loadHistory(setHistory, setHistoryLoading)}
            disabled={historyLoading}
          >
            Обновить
          </Button>
        </Box>
      </Box>
    </Box>
  );
};

export default ImportDialog;
