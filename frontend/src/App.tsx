import { useState } from 'react';
import {
  CssBaseline,
  ThemeProvider,
  createTheme,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  useTheme,
  useMediaQuery
} from '@mui/material';
import {
  Menu as MenuIcon,
  People as PeopleIcon,
  Build as BuildIcon
} from '@mui/icons-material';

import NavigationMenu from './components/NavigationMenu';
import HumanBeingDialog from './components/HumanBeingDialog';

import HumanBeingsPage from './pages/HumanBeingsPage';
import SpecialOperationsPage from './pages/SpecialOperationsPage';

import { HumanBeing } from './types';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

type PageId = 'humanbeings' | 'special-operations';

const pageConfig = {
  humanbeings: {
    title: 'Human Beings',
    icon: <PeopleIcon />,
    component: HumanBeingsPage
  },
  'special-operations': {
    title: 'Special Operations',
    icon: <BuildIcon />,
    component: SpecialOperationsPage
  }
};

function App() {
  const [currentPage, setCurrentPage] = useState<PageId>('humanbeings');
  const [menuOpen, setMenuOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedHumanBeing, setSelectedHumanBeing] = useState<HumanBeing | null>(null);
  // const [refreshTrigger, setRefreshTrigger] = useState(0);

  const muiTheme = useTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));

  const handleMenuToggle = () => {
    setMenuOpen(!menuOpen);
  };

  const handleNavigate = (pageId: string) => {
    setCurrentPage(pageId as PageId);
    if (isMobile) {
      setMenuOpen(false);
    }
  };

  // const handleAdd = () => {
  //   setSelectedHumanBeing(null);
  //   setDialogOpen(true);
  // };

  // const handleEdit = (humanBeing: HumanBeing) => {
  //   setSelectedHumanBeing(humanBeing);
  //   setDialogOpen(true);
  // };

  const handleDialogClose = () => {
    setDialogOpen(false);
    setSelectedHumanBeing(null);
  };

  const handleDialogSave = () => {
    // setRefreshTrigger(prev => prev + 1);
  };

  const CurrentPageComponent = pageConfig[currentPage].component;

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      
      <NavigationMenu
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        onNavigate={handleNavigate}
        currentPage={currentPage}
      />

      <AppBar 
        position="fixed" 
        sx={{ 
          zIndex: (theme) => theme.zIndex.drawer + 1,
          ml: isMobile ? 0 : menuOpen ? '280px' : 0,
          width: isMobile ? '100%' : menuOpen ? 'calc(100% - 280px)' : '100%',
          transition: 'all 0.3s ease'
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            onClick={handleMenuToggle}
            edge="start"
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          
          <Box display="flex" alignItems="center" sx={{ flexGrow: 1 }}>
            {pageConfig[currentPage].icon}
            <Typography variant="h6" component="div" sx={{ ml: 1 }}>
              {pageConfig[currentPage].title}
            </Typography>
          </Box>
          
          <Typography variant="body2">
            Human Being Manager
          </Typography>
        </Toolbar>
      </AppBar>

      <Box
        component="main"
        sx={{
          ml: isMobile ? 0 : menuOpen ? '280px' : 0,
          width: isMobile ? '100%' : menuOpen ? 'calc(100% - 280px)' : '100%',
          transition: 'all 0.3s ease',
          minHeight: '100vh',
          backgroundColor: 'background.default'
        }}
      >
        <Toolbar /> 

        <CurrentPageComponent 
          onRefresh={handleDialogSave}
        />

        <HumanBeingDialog
          open={dialogOpen}
          onClose={handleDialogClose}
          onSave={handleDialogSave}
          humanBeing={selectedHumanBeing}
        />
      </Box>
    </ThemeProvider>
  );
}

export default App;
