import React, { useState } from 'react';
import { 
  Box, 
  Typography, 
  Paper, 
  TextField, 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  Button,
  Grid,
  Tabs,
  Tab
} from '@mui/material';
import { Clear as ClearIcon } from '@mui/icons-material';
import HumanBeingTable from '../components/HumanBeingTable';
import HumanBeingDialog from '../components/HumanBeingDialog';
import ImportDialog from '../components/ImportDialog';
import { HumanBeing } from '../types';

interface HumanBeingsPageProps {
  onRefresh?: () => void;
}

const HumanBeingsPage: React.FC<HumanBeingsPageProps> = ({ onRefresh }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedHumanBeing, setSelectedHumanBeing] = useState<HumanBeing | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [activeTab, setActiveTab] = useState(0);

  const [filterColumn, setFilterColumn] = useState<string>('');
  const [filterValue, setFilterValue] = useState<string>('');
  const [sortColumn, setSortColumn] = useState<string>('');
  const [sortDirection, setSortDirection] = useState<string>('asc');

  const handleAdd = () => {
    setSelectedHumanBeing(null);
    setDialogOpen(true);
  };

  const handleEdit = (humanBeing: HumanBeing) => {
    setSelectedHumanBeing(humanBeing);
    setDialogOpen(true);
  };

  const handleDialogClose = () => {
    setDialogOpen(false);
    setSelectedHumanBeing(null);
  };

  const handleDialogSave = () => {
    setRefreshTrigger(prev => prev + 1);
    if (onRefresh) {
      onRefresh();
    }
  };

  const handleClearFilters = () => {
    setFilterColumn('');
    setFilterValue('');
    setSortColumn('');
    setSortDirection('asc');
    setRefreshTrigger(prev => prev + 1);
  };

  const handleApplyFilters = () => {
    setRefreshTrigger(prev => prev + 1);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Human Beings Management
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Create, view, edit, and delete Human Being records
      </Typography>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Tabs value={activeTab} onChange={(_e, newValue) => setActiveTab(newValue)}>
          <Tab label="Records" />
          <Tab label="Import" />
        </Tabs>

        {activeTab === 0 && (
        <Box>
        <Typography variant="h6" gutterBottom>
          Filter & Sort
        </Typography>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Filter Column</InputLabel>
              <Select
                value={filterColumn}
                label="Filter Column"
                onChange={(e) => setFilterColumn(e.target.value)}
              >
                <MenuItem value="">None</MenuItem>
                <MenuItem value="name">Name</MenuItem>
                <MenuItem value="soundtrackName">Soundtrack</MenuItem>
                <MenuItem value="mood">Mood</MenuItem>
                <MenuItem value="weaponType">Weapon Type</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              size="small"
              label="Filter Value"
              value={filterValue}
              onChange={(e) => setFilterValue(e.target.value)}
              disabled={!filterColumn}
            />
          </Grid>
          <Grid item xs={12} sm={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Sort Column</InputLabel>
              <Select
                value={sortColumn}
                label="Sort Column"
                onChange={(e) => setSortColumn(e.target.value)}
              >
                <MenuItem value="">None</MenuItem>
                <MenuItem value="name">Name</MenuItem>
                <MenuItem value="soundtrackName">Soundtrack</MenuItem>
                <MenuItem value="mood">Mood</MenuItem>
                <MenuItem value="weaponType">Weapon Type</MenuItem>
                <MenuItem value="impactSpeed">Impact Speed</MenuItem>
                <MenuItem value="minutesOfWaiting">Minutes Waiting</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Direction</InputLabel>
              <Select
                value={sortDirection}
                label="Direction"
                onChange={(e) => setSortDirection(e.target.value)}
                disabled={!sortColumn}
              >
                <MenuItem value="asc">Ascending</MenuItem>
                <MenuItem value="desc">Descending</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={2}>
            <Box display="flex" gap={1}>
              <Button
                variant="contained"
                size="small"
                onClick={handleApplyFilters}
                disabled={!filterColumn || !filterValue}
              >
                Apply
              </Button>
              <Button
                variant="outlined"
                size="small"
                startIcon={<ClearIcon />}
                onClick={handleClearFilters}
              >
                Clear
              </Button>
            </Box>
          </Grid>
        </Grid>
        </Box>
        )}

        {activeTab === 1 && (
          <Box sx={{ pt: 2 }}>
            <ImportDialog onImportComplete={handleDialogSave} />
          </Box>
        )}
      </Paper>

      {activeTab === 0 && (
        <>
          <HumanBeingTable
            onEdit={handleEdit}
            onAdd={handleAdd}
            refreshTrigger={refreshTrigger}
            filterColumn={filterColumn}
            filterValue={filterValue}
            sortColumn={sortColumn}
            sortDirection={sortDirection}
          />

          <HumanBeingDialog
            open={dialogOpen}
            onClose={handleDialogClose}
            onSave={handleDialogSave}
            humanBeing={selectedHumanBeing}
          />
        </>
      )}
    </Box>
  );
};

export default HumanBeingsPage;
