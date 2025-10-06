import React, { useState } from 'react';
import {
  Box,
  Typography,
  Button,
  TextField,
  Alert,
  CircularProgress,
  Grid,
  Card,
  CardContent,
  CardActions,
  Divider,
  Chip
} from '@mui/material';
import {
  Calculate as CalculateIcon,
  Search as SearchIcon,
  Delete as DeleteIcon,
  Mood as MoodIcon,
  Person as PersonIcon
} from '@mui/icons-material';
import { HumanBeing } from '../types';
import { SpecialOperationsApi } from '../services/api';

const SpecialOperationsPage: React.FC = () => {
  const [loading, setLoading] = useState<{ [key: string]: boolean }>({});
  const [results, setResults] = useState<{ [key: string]: any }>({});
  const [error, setError] = useState<string | null>(null);
  const [soundtrackSubstring, setSoundtrackSubstring] = useState<string>('');

  const setOperationLoading = (operation: string, isLoading: boolean) => {
    setLoading(prev => ({ ...prev, [operation]: isLoading }));
  };

  const setOperationResult = (operation: string, result: any) => {
    setResults(prev => ({ ...prev, [operation]: result }));
  };

  const handleSumMinutesWaiting = async () => {
    try {
      setOperationLoading('sum', true);
      setError(null);
      const sum = await SpecialOperationsApi.getSumOfMinutesWaiting();
      setOperationResult('sum', sum);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to calculate sum');
    } finally {
      setOperationLoading('sum', false);
    }
  };

  const handleMaxToothpick = async () => {
    try {
      setOperationLoading('maxToothpick', true);
      setError(null);
      const humanBeing = await SpecialOperationsApi.getMaxToothpick();
      setOperationResult('maxToothpick', humanBeing);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to get max toothpick');
    } finally {
      setOperationLoading('maxToothpick', false);
    }
  };

  const handleSoundtrackStartsWith = async () => {
    if (!soundtrackSubstring.trim()) {
      setError('Please enter a substring');
      return;
    }
    try {
      setOperationLoading('soundtrack', true);
      setError(null);
      const humanBeings = await SpecialOperationsApi.getSoundtrackStartsWith(soundtrackSubstring);
      setOperationResult('soundtrack', humanBeings);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to search soundtrack');
    } finally {
      setOperationLoading('soundtrack', false);
    }
  };

  const handleDeleteHeroesWithoutToothpicks = async () => {
    if (!window.confirm('Are you sure you want to delete all heroes without toothpicks? This action cannot be undone.')) {
      return;
    }
    try {
      setOperationLoading('deleteHeroes', true);
      setError(null);
      const deletedCount = await SpecialOperationsApi.deleteHeroesWithoutToothpicks();
      setOperationResult('deleteHeroes', deletedCount);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete heroes');
    } finally {
      setOperationLoading('deleteHeroes', false);
    }
  };

  const handleSetAllMoodToSadness = async () => {
    if (!window.confirm('Are you sure you want to set all heroes\' mood to SADNESS? This action cannot be undone.')) {
      return;
    }
    try {
      setOperationLoading('setMood', true);
      setError(null);
      const updatedCount = await SpecialOperationsApi.setAllMoodToSadness();
      setOperationResult('setMood', updatedCount);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to set mood');
    } finally {
      setOperationLoading('setMood', false);
    }
  };

  const renderHumanBeing = (humanBeing: HumanBeing) => (
    <Box key={humanBeing.id} sx={{ mb: 1, p: 1, border: '1px solid #ddd', borderRadius: 1 }}>
      <Typography variant="body2">
        <strong>{humanBeing.name}</strong> (ID: {humanBeing.id})
      </Typography>
      <Typography variant="caption" color="text.secondary">
        Soundtrack: {humanBeing.soundtrackName} | Mood: {humanBeing.mood}
      </Typography>
    </Box>
  );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Special Operations
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Perform special operations on Human Being records
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" sx={{ mb: 2 }}>
                <CalculateIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Sum Minutes Waiting</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Calculate the sum of minutesOfWaiting field for all objects
              </Typography>
            </CardContent>
            <CardActions>
              <Button
                variant="contained"
                onClick={handleSumMinutesWaiting}
                disabled={loading.sum}
                startIcon={loading.sum ? <CircularProgress size={20} /> : <CalculateIcon />}
              >
                {loading.sum ? 'Calculating...' : 'Calculate Sum'}
              </Button>
            </CardActions>
            {results.sum !== undefined && (
              <CardContent sx={{ pt: 0 }}>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="h6" color="primary">
                  Total Minutes Waiting: {results.sum}
                </Typography>
              </CardContent>
            )}
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" sx={{ mb: 2 }}>
                <PersonIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Max Toothpick</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Return one object with the maximum hasToothpick value
              </Typography>
            </CardContent>
            <CardActions>
              <Button
                variant="contained"
                onClick={handleMaxToothpick}
                disabled={loading.maxToothpick}
                startIcon={loading.maxToothpick ? <CircularProgress size={20} /> : <PersonIcon />}
              >
                {loading.maxToothpick ? 'Searching...' : 'Find Max Toothpick'}
              </Button>
            </CardActions>
            {results.maxToothpick && (
              <CardContent sx={{ pt: 0 }}>
                <Divider sx={{ mb: 2 }} />
                {renderHumanBeing(results.maxToothpick)}
                <Chip 
                  label={`Has Toothpick: ${results.maxToothpick.hasToothpick === null ? 'Unknown' : results.maxToothpick.hasToothpick ? 'Yes' : 'No'}`}
                  color={results.maxToothpick.hasToothpick ? 'success' : 'default'}
                  size="small"
                />
              </CardContent>
            )}
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" sx={{ mb: 2 }}>
                <SearchIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Soundtrack Search</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Return objects whose soundtrackName starts with a given substring
              </Typography>
              <TextField
                fullWidth
                size="small"
                label="Substring"
                value={soundtrackSubstring}
                onChange={(e) => setSoundtrackSubstring(e.target.value)}
                placeholder="Enter substring..."
              />
            </CardContent>
            <CardActions>
              <Button
                variant="contained"
                onClick={handleSoundtrackStartsWith}
                disabled={loading.soundtrack || !soundtrackSubstring.trim()}
                startIcon={loading.soundtrack ? <CircularProgress size={20} /> : <SearchIcon />}
              >
                {loading.soundtrack ? 'Searching...' : 'Search'}
              </Button>
            </CardActions>
            {results.soundtrack && (
              <CardContent sx={{ pt: 0 }}>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="h6" color="primary" sx={{ mb: 1 }}>
                  Found {results.soundtrack.length} objects
                </Typography>
                {results.soundtrack.map(renderHumanBeing)}
              </CardContent>
            )}
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" sx={{ mb: 2 }}>
                <DeleteIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Delete Heroes</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Delete all heroes without toothpicks
              </Typography>
            </CardContent>
            <CardActions>
              <Button
                variant="contained"
                color="error"
                onClick={handleDeleteHeroesWithoutToothpicks}
                disabled={loading.deleteHeroes}
                startIcon={loading.deleteHeroes ? <CircularProgress size={20} /> : <DeleteIcon />}
              >
                {loading.deleteHeroes ? 'Deleting...' : 'Delete Heroes'}
              </Button>
            </CardActions>
            {results.deleteHeroes !== undefined && (
              <CardContent sx={{ pt: 0 }}>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="h6" color="error">
                  Deleted {results.deleteHeroes} heroes
                </Typography>
              </CardContent>
            )}
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" sx={{ mb: 2 }}>
                <MoodIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Set Mood to Sadness</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Set the mood of all heroes to the most sad value (SADNESS)
              </Typography>
            </CardContent>
            <CardActions>
              <Button
                variant="contained"
                color="warning"
                onClick={handleSetAllMoodToSadness}
                disabled={loading.setMood}
                startIcon={loading.setMood ? <CircularProgress size={20} /> : <MoodIcon />}
              >
                {loading.setMood ? 'Updating...' : 'Set Mood to Sadness'}
              </Button>
            </CardActions>
            {results.setMood !== undefined && (
              <CardContent sx={{ pt: 0 }}>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="h6" color="warning.main">
                  Updated {results.setMood} heroes
                </Typography>
              </CardContent>
            )}
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default SpecialOperationsPage;
