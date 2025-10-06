import React from 'react';
import { TextField, TextFieldProps } from '@mui/material';

interface ValidatedTextFieldProps extends Omit<TextFieldProps, 'onChange'> {
  validationType?: 'name' | 'coordinateX' | 'coordinateY' | 'impactSpeed' | 'minutesOfWaiting';
  realHero?: boolean;
  maxLength?: number;
}

const ValidatedTextField: React.FC<ValidatedTextFieldProps> = ({
  validationType = 'name',
  realHero = false,
  maxLength,
  ...props
}) => {
  return (
    <TextField
      {...props}
      inputProps={{
        maxLength: maxLength,
        ...props.inputProps
      }}
    />
  );
};

export default ValidatedTextField;
