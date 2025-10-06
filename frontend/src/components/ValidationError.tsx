import React from 'react';
import { Alert, AlertTitle, Box, Typography } from '@mui/material';
import { Error as ErrorIcon, Warning as WarningIcon } from '@mui/icons-material';

interface ValidationErrorProps {
  errors: string[];
  type?: 'error' | 'warning';
  title?: string;
}

const ValidationError: React.FC<ValidationErrorProps> = ({ 
  errors, 
  type = 'error', 
  title = 'Validation Errors' 
}) => {
  if (!errors || errors.length === 0) {
    return null;
  }

  const Icon = type === 'error' ? ErrorIcon : WarningIcon;
  const severity = type === 'error' ? 'error' : 'warning';

  return (
    <Alert severity={severity} icon={<Icon />} sx={{ mb: 2 }}>
      <AlertTitle>{title}</AlertTitle>
      <Box>
        {errors.map((error, index) => (
          <Typography key={index} variant="body2" component="div">
            • {error}
          </Typography>
        ))}
      </Box>
    </Alert>
  );
};

export default ValidationError;
