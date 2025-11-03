import React, { useState, useEffect } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  IconButton,
  Typography,
  Box,
  Alert,
  CircularProgress,
  Chip,
  Pagination,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import { HumanBeing, Mood } from '../types';
import { HumanBeingApi, PaginatedResponse } from '../services/api';

interface HumanBeingTableProps {
  onEdit: (humanBeing: HumanBeing) => void;
  onAdd: () => void;
  refreshTrigger?: number;
  filterColumn?: string;
  filterValue?: string;
  sortColumn?: string;
  sortDirection?: string;
}

const HumanBeingTable: React.FC<HumanBeingTableProps> = ({ 
  onEdit, 
  onAdd, 
  refreshTrigger, 
  filterColumn, 
  filterValue, 
  sortColumn, 
  sortDirection 
}) => {
  const [humanBeings, setHumanBeings] = useState<HumanBeing[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const loadHumanBeings = async (page: number = currentPage, size: number = pageSize) => {
    try {
      setLoading(true);
      setError(null);
      const data: PaginatedResponse<HumanBeing> = await HumanBeingApi.getAllPaginatedWithFilters(
        page, 
        size, 
        filterColumn, 
        filterValue, 
        sortColumn, 
        sortDirection
      );
      setHumanBeings(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
      setCurrentPage(data.currentPage);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHumanBeings();
  }, [refreshTrigger, filterColumn, filterValue, sortColumn, sortDirection]);

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this Human Being? This action cannot be undone.')) {
      try {
        await HumanBeingApi.delete(id);
        await loadHumanBeings();
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to delete item');
      }
    }
  };

  const handlePageChange = (_event: React.ChangeEvent<unknown>, page: number) => {
    const newPage = page - 1;
    setCurrentPage(newPage);
    loadHumanBeings(newPage, pageSize);
  };

  const handlePageSizeChange = (event: any) => {
    const newSize = event.target.value;
    setPageSize(newSize);
    setCurrentPage(0);
    loadHumanBeings(0, newSize);
  };

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
      
      return dateObj.toLocaleDateString('en-US', {
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

  const getMoodColor = (mood: Mood) => {
    switch (mood) {
      case Mood.SADNESS: return 'error';
      case Mood.APATHY: return 'default';
      case Mood.CALM: return 'success';
      case Mood.RAGE: return 'warning';
      default: return 'default';
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={200}>
        <CircularProgress />
        <Typography variant="body1" sx={{ ml: 2 }}>Loading Human Beings...</Typography>
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ mb: 2 }}>
        <Alert severity="error" action={
          <Button color="inherit" size="small" onClick={() => loadHumanBeings()}>
            <RefreshIcon sx={{ mr: 1 }} />
            Retry
          </Button>
        }>
          {error}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h6" component="h2">
          Human Beings ({totalElements} total)
        </Typography>
        <Box display="flex" alignItems="center" gap={2}>
          <FormControl size="small" sx={{ minWidth: 80 }}>
            <InputLabel>Per page</InputLabel>
            <Select
              value={pageSize}
              label="Per page"
              onChange={handlePageSizeChange}
            >
              <MenuItem value={5}>5</MenuItem>
              <MenuItem value={10}>10</MenuItem>
              <MenuItem value={20}>20</MenuItem>
              <MenuItem value={50}>50</MenuItem>
            </Select>
          </FormControl>
          <Button
            startIcon={<RefreshIcon />}
            onClick={() => loadHumanBeings()}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={onAdd}
          >
            Add New
          </Button>
        </Box>
      </Box>

      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }} aria-label="human beings table">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Coordinates</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Real Hero</TableCell>
              <TableCell>Toothpick</TableCell>
              <TableCell>Car</TableCell>
              <TableCell>Mood</TableCell>
              <TableCell>Impact Speed</TableCell>
              <TableCell>Soundtrack</TableCell>
              <TableCell>Minutes Waiting</TableCell>
              <TableCell>Weapon</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {humanBeings.length === 0 ? (
              <TableRow>
                <TableCell colSpan={13} align="center">
                  <Typography color="textSecondary">
                    No Human Beings found. Click "Add New" to create one.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              humanBeings.map((humanBeing) => (
                <TableRow key={humanBeing.id} hover>
                  <TableCell>{humanBeing.id}</TableCell>
                  <TableCell>{humanBeing.name}</TableCell>
                  <TableCell>
                    ({humanBeing.coordinates?.x || 0}, {humanBeing.coordinates?.y || 0})
                  </TableCell>
                  <TableCell>{formatDate(humanBeing.creationDate)}</TableCell>
                  <TableCell>
                    <Chip 
                      label={humanBeing.realHero ? 'Yes' : 'No'}
                      color={humanBeing.realHero ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {humanBeing.hasToothpick === null ? 'Unknown' : 
                     humanBeing.hasToothpick ? 'Yes' : 'No'}
                  </TableCell>
                  <TableCell>
                    {humanBeing.car?.name || 'Unnamed'} 
                    {humanBeing.car?.cool ? ' (Cool)' : ''}
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={humanBeing.mood}
                      color={getMoodColor(humanBeing.mood)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{humanBeing.impactSpeed}</TableCell>
                  <TableCell>{humanBeing.soundtrackName}</TableCell>
                  <TableCell>{humanBeing.minutesOfWaiting}</TableCell>
                  <TableCell>{humanBeing.weaponType}</TableCell>
                  <TableCell>
                    <IconButton 
                      onClick={() => onEdit(humanBeing)}
                      color="primary"
                      size="small"
                      title="Edit"
                    >
                      <EditIcon />
                    </IconButton>
                    <IconButton 
                      onClick={() => humanBeing.id && handleDelete(humanBeing.id)}
                      color="error"
                      size="small"
                      title="Delete"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {totalPages > 1 && (
        <Box display="flex" justifyContent="center" sx={{ mt: 2 }}>
          <Pagination
            count={totalPages}
            page={currentPage + 1} 
            onChange={handlePageChange}
            color="primary"
            showFirstButton
            showLastButton
          />
        </Box>
      )}
    </Box>
  );
};

export default HumanBeingTable;